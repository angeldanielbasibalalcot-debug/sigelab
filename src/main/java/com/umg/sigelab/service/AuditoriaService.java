package com.umg.sigelab.service;

import com.umg.sigelab.exception.BusinessException;
import com.umg.sigelab.model.AccionAuditoria;
import com.umg.sigelab.model.NombreRol;
import com.umg.sigelab.model.RegistroAuditoria;
import com.umg.sigelab.repository.AuditoriaRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Reglas de negocio de la consulta de auditoria (RN-18).
 * Solo el rol ADMIN puede consultar la bitacora.
 */
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository = new AuditoriaRepository();

    private void exigirPermiso() {
        AutorizacionService.exigirRol(NombreRol.ADMIN);
    }

    public List<RegistroAuditoria> consultar(String accion, LocalDate desde, LocalDate hasta) {
        exigirPermiso();

        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new BusinessException("La fecha inicial no puede ser posterior a la fecha final.");
        }

        String accionFiltro = (accion == null || accion.isBlank()) ? null : accion.trim();
        return auditoriaRepository.consultar(accionFiltro, desde, hasta);
    }

    public List<String> listarAcciones() {
        exigirPermiso();

        List<String> acciones = new ArrayList<>();
        for (AccionAuditoria a : AccionAuditoria.values()) {
            acciones.add(a.name());
        }
        return acciones;
    }
}