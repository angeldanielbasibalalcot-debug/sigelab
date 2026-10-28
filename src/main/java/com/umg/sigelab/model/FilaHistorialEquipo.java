package com.umg.sigelab.model;

import java.time.LocalDateTime;

/**
 * Una fila del historial de prestamos de un equipo.
 * fechaDevolucion, condicion y observacion son null mientras el equipo siga pendiente.
 */
public record FilaHistorialEquipo(
        int prestamoId,
        String solicitante,
        LocalDateTime fechaPrestamo,
        LocalDateTime fechaEsperada,
        LocalDateTime fechaDevolucion,
        String condicion,
        String observacion) {
}
