package com.umg.sigelab.controller;

import com.umg.sigelab.model.Laboratorio;
import com.umg.sigelab.service.LaboratorioService;

import java.util.List;

/** Coordina LaboratorioForm con LaboratorioService. No contiene reglas de negocio. */
public class LaboratorioController {

    private final LaboratorioService laboratorioService = new LaboratorioService();

    public List<Laboratorio> listar() {
        return laboratorioService.listar();
    }

    public void crear(Laboratorio datos) {
        laboratorioService.crear(datos);
    }

    public void actualizar(Laboratorio datos) {
        laboratorioService.actualizar(datos);
    }

    public void desactivar(int idLaboratorio) {
        laboratorioService.desactivar(idLaboratorio);
    }

    public void reactivar(int idLaboratorio) {
        laboratorioService.reactivar(idLaboratorio);
    }
}