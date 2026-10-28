package com.umg.sigelab.model;

import java.time.LocalDateTime;

/**
 * Una fila del historial de mantenimiento.
 * fechaSalida y resultado son null mientras el mantenimiento este EN_PROCESO.
 */
public record FilaHistorialMantenimiento(
        int id,
        String codigoEquipo,
        String nombreEquipo,
        LocalDateTime fechaIngreso,
        String tipo,
        String descripcion,
        String responsable,
        LocalDateTime fechaSalida,
        String resultado,
        String estado) {
}
