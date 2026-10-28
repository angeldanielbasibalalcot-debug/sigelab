package com.umg.sigelab.service;

import com.umg.sigelab.exception.BusinessException;
import com.umg.sigelab.model.AccionAuditoria;
import com.umg.sigelab.model.Equipo;
import com.umg.sigelab.model.EstadoEquipo;
import com.umg.sigelab.model.EstadoPrestamo;
import com.umg.sigelab.model.NombreRol;
import com.umg.sigelab.model.Prestamo;
import com.umg.sigelab.repository.AuditoriaRepository;
import com.umg.sigelab.repository.PrestamoRepository;
import com.umg.sigelab.util.SesionActual;
import com.umg.sigelab.util.Transaccion;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * Reglas de negocio de prestamos (RN-05 a RN-09, RN-14, RN-18, RN-20).
 * La confirmacion es una sola transaccion: encabezado, detalle, estados y auditoria.
 */
public class PrestamoService {

    private final PrestamoRepository prestamoRepository = new PrestamoRepository();
    private final AuditoriaRepository auditoriaRepository = new AuditoriaRepository();

    private void exigirPermiso() {
        AutorizacionService.exigirRol(NombreRol.ADMIN, NombreRol.ENCARGADO);
    }

    public List<Equipo> listarEquiposDisponibles() {
        exigirPermiso();
        return prestamoRepository.listarEquiposDisponibles();
    }

    public int crearPrestamo(String solicitante, LocalDate fechaEsperada,
                             String observaciones, List<Integer> equipoIds) {
        exigirPermiso();

        String solicitanteLimpio = obligatorio(solicitante, "Solicitante", 120);
        String observacionesLimpias = opcional(observaciones, "Observaciones", 300);

        if (fechaEsperada == null) {
            throw new BusinessException("Indique la fecha esperada de devoluci\u00f3n.");
        }
        if (fechaEsperada.isBefore(LocalDate.now())) {
            throw new BusinessException("La fecha esperada de devoluci\u00f3n no puede ser anterior a hoy.");
        }
        // RN-05
        if (equipoIds == null || equipoIds.isEmpty()) {
            throw new BusinessException("Agregue al menos un equipo al pr\u00e9stamo.");
        }
        // RN-06
        if (new HashSet<>(equipoIds).size() != equipoIds.size()) {
            throw new BusinessException("Un mismo equipo no puede aparecer dos veces en el pr\u00e9stamo.");
        }

        int actor = SesionActual.getUsuario().getId();

        Prestamo prestamo = new Prestamo();
        prestamo.setSolicitante(solicitanteLimpio);
        prestamo.setFechaPrestamo(new Date());
        // La fecha esperada vence al final del dia elegido.
        prestamo.setFechaEsperadaDevolucion(Date.from(
                fechaEsperada.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()));
        prestamo.setUsuarioRegistraId(actor);
        prestamo.setEstado(EstadoPrestamo.ACTIVO);
        prestamo.setObservaciones(observacionesLimpias);

        return Transaccion.ejecutar(con -> {
            int idPrestamo = prestamoRepository.insertarPrestamo(con, prestamo);
            List<String> codigos = new ArrayList<>();

            for (int equipoId : equipoIds) {
                String codigo = prestamoRepository.obtenerCodigoEquipo(con, equipoId);
                if (codigo == null) {
                    throw new BusinessException("Uno de los equipos seleccionados ya no existe.");
                }
                // RN-07, RN-08, RN-14: solo DISPONIBLE puede prestarse
                String estado = prestamoRepository.obtenerEstadoEquipo(con, equipoId);
                if (!EstadoEquipo.DISPONIBLE.name().equals(estado)) {
                    throw new BusinessException("El equipo " + codigo
                            + " no se puede prestar porque est\u00e1 en estado " + estado + ".");
                }

                prestamoRepository.insertarDetalle(con, idPrestamo, equipoId);

                // RN-09: pasa a PRESTADO solo si sigue DISPONIBLE
                if (!prestamoRepository.marcarPrestadoSiDisponible(con, equipoId)) {
                    throw new BusinessException("El equipo " + codigo
                            + " acaba de ser prestado por otro usuario.");
                }
                codigos.add(codigo);
            }

            // RN-18
            auditoriaRepository.registrar(con, actor, AccionAuditoria.PRESTAMO_CREADO,
                    "Prestamo", idPrestamo,
                    "Prestamo a " + solicitanteLimpio + " con " + codigos.size()
                            + " equipo(s): " + String.join(", ", codigos) + ".");
            return idPrestamo;
        });
    }

    private String obligatorio(String valor, String campo, int max) {
        if (valor == null || valor.isBlank()) {
            throw new BusinessException("El campo " + campo + " es obligatorio.");
        }
        String limpio = valor.trim();
        if (limpio.length() > max) {
            throw new BusinessException("El campo " + campo + " no puede superar " + max + " caracteres.");
        }
        return limpio;
    }

    private String opcional(String valor, String campo, int max) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = valor.trim();
        if (limpio.length() > max) {
            throw new BusinessException("El campo " + campo + " no puede superar " + max + " caracteres.");
        }
        return limpio;
    }
}