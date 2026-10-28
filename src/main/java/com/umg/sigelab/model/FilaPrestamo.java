package com.umg.sigelab.model;

import java.time.LocalDateTime;

/**
 * Una fila de los reportes de prestamos.
 * situacion puede ser ACTIVO, FINALIZADO o VENCIDO (RN-17); la decide el Service.
 */
public record FilaPrestamo(
        int id,
        String solicitante,
        LocalDateTime fechaPrestamo,
        LocalDateTime fechaEsperada,
        String estado,
        String registradoPor,
        int totalEquipos,
        int pendientes,
        String situacion) {

    public FilaPrestamo conSituacion(String nuevaSituacion) {
        return new FilaPrestamo(id, solicitante, fechaPrestamo, fechaEsperada, estado,
                registradoPor, totalEquipos, pendientes, nuevaSituacion);
    }
}
