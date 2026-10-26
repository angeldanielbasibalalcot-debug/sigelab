package com.umg.sigelab.controller;

import com.umg.sigelab.model.Equipo;
import com.umg.sigelab.model.Laboratorio;
import com.umg.sigelab.model.TipoEquipo;
import com.umg.sigelab.service.EquipoService;

import java.util.List;

public class EquipoController {

    private final EquipoService service = new EquipoService();

    public List<Equipo> listar() {
        return service.listar();
    }

    public List<TipoEquipo> listarTipos() {
        return service.listarTipos();
    }

    public List<Laboratorio> listarLaboratorios() {
        return service.listarLaboratorios();
    }

    public Equipo buscarPorCodigo(String codigo) {
        return service.buscarPorCodigo(codigo);
    }

    public void crear(Equipo datos) {
        service.crear(datos);
    }

    public void actualizar(Equipo datos) {
        service.actualizar(datos);
    }
}