package com.umg.sigelab.service;

import com.umg.sigelab.exception.BusinessException;
import com.umg.sigelab.model.AccionAuditoria;
import com.umg.sigelab.model.EstadoLaboratorio;
import com.umg.sigelab.model.Laboratorio;
import com.umg.sigelab.model.NombreRol;
import com.umg.sigelab.repository.AuditoriaRepository;
import com.umg.sigelab.repository.LaboratorioRepository;
import com.umg.sigelab.util.SesionActual;
import com.umg.sigelab.util.Transaccion;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Reglas de negocio de laboratorios.
 *  - Solo el ADMIN crea, edita, desactiva y reactiva (RN-20: se valida aqui, no solo en el menu).
 *  - El ENCARGADO puede consultar la lista (la necesitara para registrar equipos).
 *  - Codigo unico (RN-03), campos obligatorios y capacidad positiva.
 *  - Desactivacion logica: se conserva el historial.
 *  - Cada operacion y su auditoria se guardan en la MISMA transaccion (RN-18).
 */
public class LaboratorioService {

    private static final Pattern PATRON_CODIGO = Pattern.compile("^[A-Za-z0-9._-]{2,20}$");
    private static final int CAPACIDAD_MAXIMA = 1000;

    private final LaboratorioRepository laboratorioRepository = new LaboratorioRepository();
    private final AuditoriaRepository auditoriaRepository = new AuditoriaRepository();

    public List<Laboratorio> listar() {
        AutorizacionService.exigirRol(NombreRol.ADMIN, NombreRol.ENCARGADO);
        return laboratorioRepository.listarTodos();
    }

    /** Crea un laboratorio ACTIVO. 'datos' trae codigo, nombre, ubicacion y capacidad. */
    public void crear(Laboratorio datos) {
        AutorizacionService.exigirRol(NombreRol.ADMIN);

        String codigo = texto(datos.getCodigo()).toUpperCase();
        String nombre = texto(datos.getNombre());
        String ubicacion = texto(datos.getUbicacion());
        validarDatos(codigo, nombre, ubicacion, datos.getCapacidad());

        if (laboratorioRepository.existeCodigo(codigo, 0)) {
            throw new BusinessException("Ya existe un laboratorio con el código " + codigo + ".");
        }

        Laboratorio nuevo = new Laboratorio(0, codigo, nombre, ubicacion,
                datos.getCapacidad(), EstadoLaboratorio.ACTIVO);
        int actor = SesionActual.getUsuario().getId();

        Transaccion.ejecutarSinResultado(con -> {
            int id = laboratorioRepository.insertar(con, nuevo);
            auditoriaRepository.registrar(con, actor, AccionAuditoria.LABORATORIO_CREADO, "Laboratorio", id,
                    "Laboratorio creado: " + codigo + " - " + nombre);
        });
    }

    /** Actualiza codigo, nombre, ubicacion y capacidad. El estado no cambia aqui. */
    public void actualizar(Laboratorio datos) {
        AutorizacionService.exigirRol(NombreRol.ADMIN);

        String codigo = texto(datos.getCodigo()).toUpperCase();
        String nombre = texto(datos.getNombre());
        String ubicacion = texto(datos.getUbicacion());
        validarDatos(codigo, nombre, ubicacion, datos.getCapacidad());

        Laboratorio actual = buscarExistente(datos.getId());

        if (laboratorioRepository.existeCodigo(codigo, actual.getId())) {
            throw new BusinessException("Ya existe un laboratorio con el código " + codigo + ".");
        }

        Laboratorio cambios = new Laboratorio(actual.getId(), codigo, nombre, ubicacion,
                datos.getCapacidad(), actual.getEstado());
        int actor = SesionActual.getUsuario().getId();

        Transaccion.ejecutarSinResultado(con -> {
            laboratorioRepository.actualizar(con, cambios);
            auditoriaRepository.registrar(con, actor, AccionAuditoria.LABORATORIO_ACTUALIZADO, "Laboratorio",
                    cambios.getId(), "Laboratorio actualizado: " + codigo + " - " + nombre);
        });
    }

    /** Desactivacion logica: el laboratorio se conserva, pero ya no debe usarse para equipos nuevos. */
    public void desactivar(int idLaboratorio) {
        cambiarEstado(idLaboratorio, EstadoLaboratorio.INACTIVO, AccionAuditoria.LABORATORIO_DESACTIVADO);
    }

    public void reactivar(int idLaboratorio) {
        cambiarEstado(idLaboratorio, EstadoLaboratorio.ACTIVO, AccionAuditoria.LABORATORIO_REACTIVADO);
    }

    // ---------------- privados ----------------

    private void cambiarEstado(int idLaboratorio, EstadoLaboratorio nuevoEstado, AccionAuditoria accion) {
        AutorizacionService.exigirRol(NombreRol.ADMIN);

        Laboratorio lab = buscarExistente(idLaboratorio);

        if (lab.getEstado() == nuevoEstado) {
            throw new BusinessException("El laboratorio " + lab.getCodigo() + " ya está "
                    + (nuevoEstado == EstadoLaboratorio.INACTIVO ? "inactivo." : "activo."));
        }

        int actor = SesionActual.getUsuario().getId();

        Transaccion.ejecutarSinResultado(con -> {
            laboratorioRepository.cambiarEstado(con, lab.getId(), nuevoEstado);
            auditoriaRepository.registrar(con, actor, accion, "Laboratorio", lab.getId(),
                    (nuevoEstado == EstadoLaboratorio.INACTIVO ? "Laboratorio desactivado: " : "Laboratorio reactivado: ")
                            + lab.getCodigo());
        });
    }

    private Laboratorio buscarExistente(int id) {
        return laboratorioRepository.buscarPorId(id)
                .orElseThrow(() -> new BusinessException("El laboratorio seleccionado ya no existe."));
    }

    private void validarDatos(String codigo, String nombre, String ubicacion, int capacidad) {
        if (codigo.isEmpty()) {
            throw new BusinessException("Ingrese el código del laboratorio.");
        }
        if (!PATRON_CODIGO.matcher(codigo).matches()) {
            throw new BusinessException("El código debe tener entre 2 y 20 caracteres y solo puede "
                    + "contener letras, números, punto, guion y guion bajo (por ejemplo LAB-01).");
        }
        if (nombre.isEmpty()) {
            throw new BusinessException("Ingrese el nombre del laboratorio.");
        }
        if (nombre.length() > 100) {
            throw new BusinessException("El nombre no puede superar los 100 caracteres.");
        }
        if (ubicacion.isEmpty()) {
            throw new BusinessException("Ingrese la ubicación del laboratorio.");
        }
        if (ubicacion.length() > 150) {
            throw new BusinessException("La ubicación no puede superar los 150 caracteres.");
        }
        if (capacidad <= 0) {
            throw new BusinessException("La capacidad debe ser un número mayor que cero.");
        }
        if (capacidad > CAPACIDAD_MAXIMA) {
            throw new BusinessException("La capacidad no puede superar " + CAPACIDAD_MAXIMA + ".");
        }
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }
}