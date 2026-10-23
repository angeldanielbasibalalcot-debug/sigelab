package com.umg.sigelab.controller;

import com.umg.sigelab.model.TipoEquipo;
import com.umg.sigelab.service.TipoEquipoService;

import java.util.List;

/** Coordina TipoEquipoForm con TipoEquipoService. No contiene reglas de negocio. */
public class TipoEquipoController {

    private final TipoEquipoService tipoEquipoService = new TipoEquipoService();

    public List<TipoEquipo> listar() {
        return tipoEquipoService.listar();
    }

    public void crear(TipoEquipo datos) {
        tipoEquipoService.crear(datos);
    }

    public void actualizar(TipoEquipo datos) {
        tipoEquipoService.actualizar(datos);
    }
}