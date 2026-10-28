package com.umg.sigelab.service;

import com.umg.sigelab.exception.BusinessException;
import com.umg.sigelab.model.AccionAuditoria;
import com.umg.sigelab.model.CondicionDevolucion;
import com.umg.sigelab.model.EquipoPrestado;
import com.umg.sigelab.model.NombreRol;
import com.umg.sigelab.repository.AuditoriaRepository;
import com.umg.sigelab.repository.DevolucionRepository;
import com.umg.sigelab.util.SesionActual;
import com.umg.sigelab.util.Transaccion;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reglas de negocio de las devoluciones (RN-10 a RN-13, RN-16, RN-18, RN-20).
 */
public class DevolucionService {

    private static final int MAX_OBSERVACION = 255;

    private final DevolucionRepository devolucionRepository = new DevolucionRepository();
    private final AuditoriaRepository auditoriaRepository = new AuditoriaRepository();

    public List<EquipoPrestado> listarPendientes() {
        exigirPermiso();
        return devolucionRepository.listarPendientes();
    }

    /**
     * Devuelve un equipo de un prestamo. Todo ocurre en una sola transaccion:
     * detalle, estado del equipo, cierre del prestamo y auditoria.
     * Retorna true si con esta devolucion el prestamo quedo FINALIZADO.
     */
    public boolean devolverEquipo(int detalleId, CondicionDevolucion condicion, String observacion) {
        exigirPermiso();
        if (condicion == null) {
            throw new BusinessException("Seleccione la condici\u00f3n en que regresa el equipo.");
        }
        String obs = opcional(observacion, "La observaci\u00f3n", MAX_OBSERVACION);
        int actor = SesionActual.getUsuario().getId();

        return Transaccion.ejecutar(con -> {
            EquipoPrestado detalle = devolucionRepository.obtenerDetalle(con, detalleId);
            if (detalle == null) {
                throw new BusinessException("El equipo seleccionado no pertenece a ning\u00fan pr\u00e9stamo.");
            }
            // RN-11: no se devuelve dos veces
            if (detalle.devuelto()) {
                throw new BusinessException("El equipo " + detalle.codigoInventario()
                        + " ya fue devuelto en el pr\u00e9stamo " + detalle.prestamoId() + ".");
            }
            // RN-10: solo dentro de un prestamo activo
            if (!"ACTIVO".equals(detalle.estadoPrestamo())) {
                throw new BusinessException("El pr\u00e9stamo " + detalle.prestamoId()
                        + " ya no est\u00e1 activo.");
            }

            int filasDetalle = devolucionRepository.registrarDevolucion(
                    con, detalleId, condicion.name(), obs, actor, LocalDateTime.now());
            if (filasDetalle != 1) {
                throw new BusinessException("El equipo " + detalle.codigoInventario()
                        + " ya fue devuelto.");
            }

            // RN-12 (BUENO -> DISPONIBLE) y RN-13 (DANADO -> DANADO)
            String nuevoEstado = (condicion == CondicionDevolucion.BUENO) ? "DISPONIBLE" : "DANADO";
            int filasEquipo = devolucionRepository.actualizarEstadoEquipoPrestado(
                    con, detalle.equipoId(), nuevoEstado);
            if (filasEquipo != 1) {
                throw new BusinessException("El equipo " + detalle.codigoInventario()
                        + " no figura como PRESTADO, no se puede devolver.");
            }

            // RN-16: el prestamo se finaliza solo si no quedan equipos pendientes
            boolean finalizado = false;
            if (devolucionRepository.contarPendientes(con, detalle.prestamoId()) == 0) {
                devolucionRepository.finalizarPrestamo(con, detalle.prestamoId());
                finalizado = true;
            }

            // RN-18
            auditoriaRepository.registrar(con, actor, AccionAuditoria.EQUIPO_DEVUELTO,
                    "Prestamo", detalle.prestamoId(),
                    "Devolucion de " + detalle.codigoInventario() + " como " + condicion.name()
                            + (finalizado ? ". Prestamo finalizado." : "."));
            return finalizado;
        });
    }

    private void exigirPermiso() {
        AutorizacionService.exigirRol(NombreRol.ADMIN, NombreRol.ENCARGADO);
    }

    private String opcional(String valor, String campo, int max) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        String limpio = valor.trim();
        if (limpio.length() > max) {
            throw new BusinessException(campo + " no puede superar " + max + " caracteres.");
        }
        return limpio;
    }
}