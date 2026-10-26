package com.umg.sigelab.service;

import com.umg.sigelab.exception.BusinessException;
import com.umg.sigelab.model.AccionAuditoria;
import com.umg.sigelab.model.NombreRol;
import com.umg.sigelab.model.TipoEquipo;
import com.umg.sigelab.repository.AuditoriaRepository;
import com.umg.sigelab.repository.TipoEquipoRepository;
import com.umg.sigelab.util.SesionActual;
import com.umg.sigelab.util.Transaccion;

import java.util.List;

/**
 * Reglas de negocio del catalogo de tipos de equipo.
 *  - Solo el ADMIN crea y edita (RN-20: se valida aqui, no solo en el menu).
 *  - El ENCARGADO puede consultar la lista (la necesitara para registrar equipos).
 *  - Nombre obligatorio y unico; la descripcion es opcional.
 *  - Cada operacion y su auditoria se guardan en la MISMA transaccion (RN-18).
 */
public class TipoEquipoService {

    private final TipoEquipoRepository tipoEquipoRepository = new TipoEquipoRepository();
    private final AuditoriaRepository auditoriaRepository = new AuditoriaRepository();

    public List<TipoEquipo> listar() {
        AutorizacionService.exigirRol(NombreRol.ADMIN, NombreRol.ENCARGADO);
        return tipoEquipoRepository.listarTodos();
    }

    /** Crea un tipo de equipo. 'datos' trae nombre y descripcion (opcional). */
    public void crear(TipoEquipo datos) {
        AutorizacionService.exigirRol(NombreRol.ADMIN);

        String nombre = texto(datos.getNombre());
        String descripcion = descripcionONulo(datos.getDescripcion());
        validarDatos(nombre, descripcion);

        if (tipoEquipoRepository.existeNombre(nombre, 0)) {
            throw new BusinessException("Ya existe un tipo de equipo llamado " + nombre + ".");
        }

        TipoEquipo nuevo = new TipoEquipo(0, nombre, descripcion);
        int actor = SesionActual.getUsuario().getId();

        Transaccion.ejecutarSinResultado(con -> {
            int id = tipoEquipoRepository.insertar(con, nuevo);
            auditoriaRepository.registrar(con, actor, AccionAuditoria.TIPO_EQUIPO_CREADO, "TipoEquipo", id,
                    "Tipo de equipo creado: " + nombre);
        });
    }

    /** Actualiza nombre y descripcion. */
    public void actualizar(TipoEquipo datos) {
        AutorizacionService.exigirRol(NombreRol.ADMIN);

        String nombre = texto(datos.getNombre());
        String descripcion = descripcionONulo(datos.getDescripcion());
        validarDatos(nombre, descripcion);

        TipoEquipo actual = tipoEquipoRepository.buscarPorId(datos.getId())
                .orElseThrow(() -> new BusinessException("El tipo de equipo seleccionado ya no existe."));

        if (tipoEquipoRepository.existeNombre(nombre, actual.getId())) {
            throw new BusinessException("Ya existe un tipo de equipo llamado " + nombre + ".");
        }

        TipoEquipo cambios = new TipoEquipo(actual.getId(), nombre, descripcion);
        int actor = SesionActual.getUsuario().getId();

        Transaccion.ejecutarSinResultado(con -> {
            tipoEquipoRepository.actualizar(con, cambios);
            auditoriaRepository.registrar(con, actor, AccionAuditoria.TIPO_EQUIPO_ACTUALIZADO, "TipoEquipo",
                    cambios.getId(), "Tipo de equipo actualizado: " + nombre);
        });
    }

    // ---------------- privados ----------------

    private void validarDatos(String nombre, String descripcion) {
        if (nombre.isEmpty()) {
            throw new BusinessException("Ingrese el nombre del tipo de equipo.");
        }
        if (nombre.length() > 50) {
            throw new BusinessException("El nombre no puede superar los 50 caracteres.");
        }
        if (descripcion != null && descripcion.length() > 200) {
            throw new BusinessException("La descripción no puede superar los 200 caracteres.");
        }
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    /** La descripcion es opcional: si viene vacia se guarda NULL en la BD. */
    private String descripcionONulo(String valor) {
        String limpio = texto(valor);
        return limpio.isEmpty() ? null : limpio;
    }
}
