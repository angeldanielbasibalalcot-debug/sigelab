package com.umg.sigelab.controller;

import com.umg.sigelab.model.Equipo;
import com.umg.sigelab.model.Mantenimiento;
import com.umg.sigelab.model.ResultadoMantenimiento;
import com.umg.sigelab.model.TipoMantenimiento;
import com.umg.sigelab.service.MantenimientoService;

import java.util.List;

public class MantenimientoController {

    private final MantenimientoService service = new MantenimientoService();

    public List<Mantenimiento> listar() {
        return service.listar();
    }

    public List<Mantenimiento> listarEnProceso() {
        return service.listarEnProceso();
    }

    public List<Mantenimiento> listarPorEquipo(int equipoId) {
        return service.listarPorEquipo(equipoId);
    }

    public List<Equipo> listarEquiposElegibles() {
        return service.listarEquiposElegibles();
    }

    public void enviar(int equipoId, TipoMantenimiento tipo,
                       String descripcion, String responsable,
                       String observaciones) {
        service.enviar(equipoId, tipo, descripcion, responsable, observaciones);
    }

    public void finalizar(int mantenimientoId,
                          ResultadoMantenimiento resultado,
                          String observaciones) {
        service.finalizar(mantenimientoId, resultado, observaciones);
    }
}