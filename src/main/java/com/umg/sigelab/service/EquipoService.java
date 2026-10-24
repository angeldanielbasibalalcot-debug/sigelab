package com.umg.sigelab.service;

import com.umg.sigelab.exception.BusinessException;
import com.umg.sigelab.model.AccionAuditoria;
import com.umg.sigelab.model.Equipo;
import com.umg.sigelab.model.EstadoEquipo;
import com.umg.sigelab.model.EstadoLaboratorio;
import com.umg.sigelab.model.Laboratorio;
import com.umg.sigelab.model.NombreRol;
import com.umg.sigelab.model.TipoEquipo;
import com.umg.sigelab.repository.AuditoriaRepository;
import com.umg.sigelab.repository.EquipoRepository;
import com.umg.sigelab.repository.LaboratorioRepository;
import com.umg.sigelab.repository.TipoEquipoRepository;
import com.umg.sigelab.util.SesionActual;
import com.umg.sigelab.util.Transaccion;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Reglas de negocio de equipos.
 *  - ADMIN y ENCARGADO gestionan equipos (RN-20: se valida aqui, no solo en el menu).
 *  - Codigo de inventario unico (RN-04), campos obligatorios y longitudes de la BD.
 *  - Tipo y laboratorio son relaciones: deben existir; un equipo nuevo solo va a un laboratorio ACTIVO.
 *  - Todo equipo nuevo nace DISPONIBLE. El estado NO se edita aqui: lo cambian los procesos de
 *    prestamo, devolucion y mantenimiento.
 *  - El codigo no se modifica una vez creado, para no invalidar el QR ya impreso.
 *  - Buscar por codigo (lectura del QR) solo consulta: no modifica estados (RN-19).
 *  - Cada operacion y su auditoria se guardan en la MISMA transaccion (RN-18).
 */
public class EquipoService {

    private static final Pattern PATRON_CODIGO = Pattern.compile("^[A-Za-z0-9._-]{2,30}$");

    private final EquipoRepository equipoRepository = new EquipoRepository();
    private final TipoEquipoRepository tipoEquipoRepository = new TipoEquipoRepository();
    private final LaboratorioRepository laboratorioRepository = new LaboratorioRepository();
    private final AuditoriaRepository auditoriaRepository = new AuditoriaRepository();

    public List<Equipo> listar() {
        AutorizacionService.exigirRol(NombreRol.ADMIN, NombreRol.ENCARGADO);
        return equipoRepository.listarTodos();
    }

    /** Tipos para el combo del formulario. */
    public List<TipoEquipo> listarTipos() {
        AutorizacionService.exigirRol(NombreRol.ADMIN, NombreRol.ENCARGADO);
        return tipoEquipoRepository.listarTodos();
    }

    /** Laboratorios para el combo del formulario: solo los ACTIVOS. */
    public List<Laboratorio> listarLaboratoriosActivos() {
        AutorizacionService.exigirRol(NombreRol.ADMIN, NombreRol.ENCARGADO);
        return laboratorioRepository.listarTodos().stream()
                .filter(l -> l.getEstado() == EstadoLaboratorio.ACTIVO)
                .toList();
    }

    /** Identifica un equipo a partir de su codigo (lo usa la lectura del QR). Solo consulta (RN-19). */
    public Equipo buscarPorCodigo(String codigoLeido) {
        AutorizacionService.exigirRol(NombreRol.ADMIN, NombreRol.ENCARGADO);
        String codigo = texto(codigoLeido).toUpperCase();
        if (codigo.isEmpty()) {
            throw new BusinessException("El código del equipo está vacío.");
        }
        return equipoRepository.buscarPorCodigo(codigo)
                .orElseThrow(() -> new BusinessException("No existe un equipo con el código " + codigo + "."));
    }

    /** Crea un equipo DISPONIBLE. 'datos' trae codigo, nombre, tipo, laboratorio y datos opcionales. */
    public void crear(Equipo datos) {
        AutorizacionService.exigirRol(NombreRol.ADMIN, NombreRol.ENCARGADO);

        String codigo = texto(datos.getCodigoInventario()).toUpperCase();
        String nombre = texto(datos.getNombre());
        String marca = opcional(datos.getMarca());
        String modelo = opcional(datos.getModelo());
        String serie = opcional(datos.getNumeroSerie());
        String observaciones = opcional(datos.getObservaciones());
        validarDatos(codigo, nombre, datos.getTipo(), datos.getLaboratorio(), marca, modelo, serie, observaciones);

        if (equipoRepository.existeCodigo(codigo, 0)) {
            throw new BusinessException("Ya existe un equipo con el código " + codigo + ".");
        }

        TipoEquipo tipo = buscarTipo(datos.getTipo().getId());
        Laboratorio laboratorio = buscarLaboratorio(datos.getLaboratorio().getId());
        if (laboratorio.getEstado() != EstadoLaboratorio.ACTIVO) {
            throw new BusinessException("El laboratorio " + laboratorio.getCodigo()
                    + " está inactivo. Elija un laboratorio activo.");
        }

        Equipo nuevo = new Equipo(0, codigo, nombre, tipo, marca, modelo, serie, laboratorio,
                EstadoEquipo.DISPONIBLE, observaciones);
        int actor = SesionActual.getUsuario().getId();

        Transaccion.ejecutarSinResultado(con -> {
            int id = equipoRepository.insertar(con, nuevo);
            auditoriaRepository.registrar(con, actor, AccionAuditoria.EQUIPO_CREADO, "Equipo", id,
                    "Equipo creado: " + codigo + " - " + nombre);
        });
    }

    /** Actualiza los datos del equipo. El codigo y el estado no se modifican aqui. */
    public void actualizar(Equipo datos) {
        AutorizacionService.exigirRol(NombreRol.ADMIN, NombreRol.ENCARGADO);

        Equipo actual = equipoRepository.buscarPorId(datos.getId())
                .orElseThrow(() -> new BusinessException("El equipo seleccionado ya no existe."));

        String nombre = texto(datos.getNombre());
        String marca = opcional(datos.getMarca());
        String modelo = opcional(datos.getModelo());
        String serie = opcional(datos.getNumeroSerie());
        String observaciones = opcional(datos.getObservaciones());
        validarDatos(actual.getCodigoInventario(), nombre, datos.getTipo(), datos.getLaboratorio(),
                marca, modelo, serie, observaciones);

        TipoEquipo tipo = buscarTipo(datos.getTipo().getId());
        Laboratorio laboratorio = buscarLaboratorio(datos.getLaboratorio().getId());
        // Solo se exige laboratorio ACTIVO si el equipo se esta moviendo a otro laboratorio.
        boolean cambiaLaboratorio = laboratorio.getId() != actual.getLaboratorio().getId();
        if (cambiaLaboratorio && laboratorio.getEstado() != EstadoLaboratorio.ACTIVO) {
            throw new BusinessException("El laboratorio " + laboratorio.getCodigo()
                    + " está inactivo. Elija un laboratorio activo.");
        }

        Equipo cambios = new Equipo(actual.getId(), actual.getCodigoInventario(), nombre, tipo, marca,
                modelo, serie, laboratorio, actual.getEstado(), observaciones);
        int actor = SesionActual.getUsuario().getId();

        Transaccion.ejecutarSinResultado(con -> {
            equipoRepository.actualizar(con, cambios);
            auditoriaRepository.registrar(con, actor, AccionAuditoria.EQUIPO_ACTUALIZADO, "Equipo",
                    cambios.getId(), "Equipo actualizado: " + cambios.getCodigoInventario() + " - " + nombre);
        });
    }

    // ---------------- privados ----------------

    private TipoEquipo buscarTipo(int id) {
        return tipoEquipoRepository.buscarPorId(id)
                .orElseThrow(() -> new BusinessException("El tipo de equipo seleccionado ya no existe."));
    }

    private Laboratorio buscarLaboratorio(int id) {
        return laboratorioRepository.buscarPorId(id)
                .orElseThrow(() -> new BusinessException("El laboratorio seleccionado ya no existe."));
    }

    private void validarDatos(String codigo, String nombre, TipoEquipo tipo, Laboratorio laboratorio,
                              String marca, String modelo, String serie, String observaciones) {
        if (codigo.isEmpty()) {
            throw new BusinessException("Ingrese el código de inventario del equipo.");
        }
        if (!PATRON_CODIGO.matcher(codigo).matches()) {
            throw new BusinessException("El código debe tener entre 2 y 30 caracteres y solo puede "
                    + "contener letras, números, punto, guion y guion bajo (por ejemplo CAM-001).");
        }
        if (nombre.isEmpty()) {
            throw new BusinessException("Ingrese el nombre del equipo.");
        }
        if (nombre.length() > 100) {
            throw new BusinessException("El nombre no puede superar los 100 caracteres.");
        }
        if (tipo == null) {
            throw new BusinessException("Seleccione el tipo de equipo.");
        }
        if (laboratorio == null) {
            throw new BusinessException("Seleccione el laboratorio del equipo.");
        }
        if (marca != null && marca.length() > 50) {
            throw new BusinessException("La marca no puede superar los 50 caracteres.");
        }
        if (modelo != null && modelo.length() > 50) {
            throw new BusinessException("El modelo no puede superar los 50 caracteres.");
        }
        if (serie != null && serie.length() > 80) {
            throw new BusinessException("El número de serie no puede superar los 80 caracteres.");
        }
        if (observaciones != null && observaciones.length() > 300) {
            throw new BusinessException("Las observaciones no pueden superar los 300 caracteres.");
        }
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    /** Los campos opcionales vacios se guardan como NULL. */
    private String opcional(String valor) {
        String limpio = texto(valor);
        return limpio.isEmpty() ? null : limpio;
    }
}