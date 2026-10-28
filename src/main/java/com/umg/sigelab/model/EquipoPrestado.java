package com.umg.sigelab.model;

import java.time.LocalDateTime;

/**
 * Una linea de DetallePrestamo con los datos del equipo y del prestamo.
 * Sirve para listar pendientes de devolucion y para validar una devolucion.
 */
public record EquipoPrestado(
        int detalleId,
        int prestamoId,
        int equipoId,
        String solicitante,
        String codigoInventario,
        String nombreEquipo,
        LocalDateTime fechaEsperada,
        boolean devuelto,
        String estadoPrestamo) {
}