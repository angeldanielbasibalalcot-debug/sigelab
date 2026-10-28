package com.umg.sigelab.controller;

import com.umg.sigelab.model.ConteoEstado;
import com.umg.sigelab.model.FilaHistorialEquipo;
import com.umg.sigelab.model.FilaHistorialMantenimiento;
import com.umg.sigelab.model.FilaPrestamo;
import com.umg.sigelab.model.ResumenDashboard;
import com.umg.sigelab.service.ReporteService;

import java.time.LocalDate;
import java.util.List;

/**
 * Coordina las acciones del dashboard y los reportes.
 * No contiene reglas de negocio ni SQL: solo delega al Service.
 */
public class ReporteController {

    private final ReporteService reporteService = new ReporteService();

    public ResumenDashboard obtenerResumen() {
        return reporteService.obtenerResumen();
    }

    public List<ConteoEstado> equiposPorEstado() {
        return reporteService.equiposPorEstado();
    }

    public List<FilaPrestamo> listarPrestamos(String situacion) {
        return reporteService.listarPrestamos(situacion);
    }

    public List<FilaPrestamo> prestamosEntreFechas(LocalDate desde, LocalDate hasta) {
        return reporteService.prestamosEntreFechas(desde, hasta);
    }

    public List<FilaHistorialEquipo> historialDeEquipo(String codigo) {
        return reporteService.historialDeEquipo(codigo);
    }

    public List<FilaHistorialMantenimiento> historialMantenimiento(String codigo) {
        return reporteService.historialMantenimiento(codigo);
    }
}
