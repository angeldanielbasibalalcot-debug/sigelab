package com.umg.sigelab.controller;

import com.umg.sigelab.model.Equipo;
import com.umg.sigelab.model.Laboratorio;
import com.umg.sigelab.model.TipoEquipo;
import com.umg.sigelab.service.EquipoService;

import java.util.List;

/** Coordina EquipoForm con EquipoService. No contiene reglas de negocio. */
public class EquipoController {

    private final EquipoService equipoService = new EquipoService();

    public List<Equipo> listar() {
        return equipoService.listar();
    }

    public List<TipoEquipo> listarTipos() {
        return equipoService.listarTipos();
    }

    public List<Laboratorio> listarLaboratoriosActivos() {
        return equipoService.listarLaboratoriosActivos();
    }

    /** Identifica un equipo por su codigo (lo usara la lectura del QR). Solo consulta. */
    public Equipo buscarPorCodigo(String codigo) {
        return equipoService.buscarPorCodigo(codigo);
    }

    public void crear(Equipo datos) {
        equipoService.crear(datos);
    }

    public void actualizar(Equipo datos) {
        equipoService.actualizar(datos);
    }
}