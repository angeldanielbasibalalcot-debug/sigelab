package com.umg.sigelab.model;

/**
 * Cantidades del dashboard (6.11). Todas salen de la base de datos.
 */
public record ResumenDashboard(
        int totalEquipos,
        int disponibles,
        int prestados,
        int danados,
        int mantenimiento,
        int baja,
        int prestamosActivos,
        int prestamosVencidos) {
}
