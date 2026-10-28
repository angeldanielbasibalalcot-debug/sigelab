package com.umg.sigelab.model;

/**
 * Una fila del reporte "equipos por estado" (GROUP BY).
 */
public record ConteoEstado(String estado, int total) {
}
