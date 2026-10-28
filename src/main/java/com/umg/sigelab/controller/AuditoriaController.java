package com.umg.sigelab.controller;

import com.umg.sigelab.model.RegistroAuditoria;
import com.umg.sigelab.service.AuditoriaService;

import java.time.LocalDate;
import java.util.List;

/**
 * Coordina las acciones de la pantalla de auditoria.
 * No contiene reglas de negocio ni SQL: solo delega al Service.
 */
public class AuditoriaController {

    private final AuditoriaService auditoriaService = new AuditoriaService();

    public List<RegistroAuditoria> consultar(String accion, LocalDate desde, LocalDate hasta) {
        return auditoriaService.consultar(accion, desde, hasta);
    }

    public List<String> listarAcciones() {
        return auditoriaService.listarAcciones();
    }
}