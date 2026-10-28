package com.umg.sigelab.service;

import com.umg.sigelab.exception.BusinessException;
import com.umg.sigelab.model.ConteoEstado;
import com.umg.sigelab.model.FilaHistorialEquipo;
import com.umg.sigelab.model.FilaHistorialMantenimiento;
import com.umg.sigelab.model.FilaPrestamo;
import com.umg.sigelab.model.NombreRol;
import com.umg.sigelab.model.ResumenDashboard;
import com.umg.sigelab.repository.ReporteRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Reglas de negocio del dashboard y los reportes (6.11, RN-17, RN-20).
 * La fecha de hoy se toma de Java (no de SQL Server, que guarda en UTC).
 */
public class ReporteService {

    public static final String ACTIVO = "ACTIVO";
    public static final String FINALIZADO = "FINALIZADO";
    public static final String VENCIDO = "VENCIDO";

    private final ReporteRepository reporteRepository = new ReporteRepository();

    private void exigirPermiso() {
        AutorizacionService.exigirRol(NombreRol.ADMIN, NombreRol.ENCARGADO, NombreRol.DOCENTE);
    }

    /** RN-17: vencido = ACTIVO, con equipos pendientes y fecha esperada anterior a hoy. */
    private boolean estaVencido(FilaPrestamo p, LocalDate hoy) {
        return ACTIVO.equals(p.estado())
                && p.pendientes() > 0
                && p.fechaEsperada().toLocalDate().isBefore(hoy);
    }

    private List<FilaPrestamo> clasificar(List<FilaPrestamo> prestamos) {
        LocalDate hoy = LocalDate.now();
        List<FilaPrestamo> resultado = new ArrayList<>();
        for (FilaPrestamo p : prestamos) {
            String situacion = estaVencido(p, hoy) ? VENCIDO : p.estado();
            resultado.add(p.conSituacion(situacion));
        }
        return resultado;
    }

    public ResumenDashboard obtenerResumen() {
        exigirPermiso();

        int disponibles = 0;
        int prestados = 0;
        int danados = 0;
        int mantenimiento = 0;
        int baja = 0;
        int total = 0;
        for (ConteoEstado c : reporteRepository.contarEquiposPorEstado()) {
            total += c.total();
            switch (c.estado()) {
                case "DISPONIBLE" -> disponibles = c.total();
                case "PRESTADO" -> prestados = c.total();
                case "DANADO" -> danados = c.total();
                case "MANTENIMIENTO" -> mantenimiento = c.total();
                case "BAJA" -> baja = c.total();
                default -> { }
            }
        }

        LocalDate hoy = LocalDate.now();
        int vencidos = 0;
        for (FilaPrestamo p : reporteRepository.listarPrestamos()) {
            if (estaVencido(p, hoy)) {
                vencidos++;
            }
        }

        return new ResumenDashboard(total, disponibles, prestados, danados, mantenimiento,
                baja, reporteRepository.contarPrestamosActivos(), vencidos);
    }

    public List<ConteoEstado> equiposPorEstado() {
        exigirPermiso();
        return reporteRepository.contarEquiposPorEstado();
    }

    /** Prestamos filtrados por situacion: ACTIVO, FINALIZADO, VENCIDO o null (todos). */
    public List<FilaPrestamo> listarPrestamos(String situacion) {
        exigirPermiso();
        List<FilaPrestamo> todos = clasificar(reporteRepository.listarPrestamos());
        if (situacion == null || situacion.isBlank()) {
            return todos;
        }
        List<FilaPrestamo> filtrados = new ArrayList<>();
        for (FilaPrestamo p : todos) {
            if (p.situacion().equals(situacion)) {
                filtrados.add(p);
            }
        }
        return filtrados;
    }

    public List<FilaPrestamo> prestamosEntreFechas(LocalDate desde, LocalDate hasta) {
        exigirPermiso();
        if (desde == null || hasta == null) {
            throw new BusinessException("Ingrese la fecha inicial y la fecha final.");
        }
        if (desde.isAfter(hasta)) {
            throw new BusinessException("La fecha inicial no puede ser posterior a la fecha final.");
        }
        return clasificar(reporteRepository.listarPrestamosEntreFechas(desde, hasta));
    }

    public List<FilaHistorialEquipo> historialDeEquipo(String codigo) {
        exigirPermiso();
        if (codigo == null || codigo.isBlank()) {
            throw new BusinessException("Ingrese el c\u00f3digo del equipo.");
        }
        String limpio = codigo.trim().toUpperCase();
        if (limpio.length() > 30) {
            throw new BusinessException("El c\u00f3digo del equipo no puede superar 30 caracteres.");
        }
        return reporteRepository.historialDeEquipo(limpio);
    }

    /** Con codigo vacio devuelve el historial de mantenimiento de todos los equipos. */
    public List<FilaHistorialMantenimiento> historialMantenimiento(String codigo) {
        exigirPermiso();
        String filtro = (codigo == null || codigo.isBlank()) ? null : codigo.trim().toUpperCase();
        if (filtro != null && filtro.length() > 30) {
            throw new BusinessException("El c\u00f3digo del equipo no puede superar 30 caracteres.");
        }
        return reporteRepository.historialMantenimiento(filtro);
    }
}
