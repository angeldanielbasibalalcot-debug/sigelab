package com.umg.sigelab.controller;

import com.umg.sigelab.model.CondicionDevolucion;
import com.umg.sigelab.model.EquipoPrestado;
import com.umg.sigelab.service.DevolucionService;

import java.util.List;

/**
 * Coordina las acciones de la pantalla de devoluciones. Solo delega al Service.
 */
public class DevolucionController {

    private final DevolucionService service = new DevolucionService();

    public List<EquipoPrestado> listarPendientes() {
        return service.listarPendientes();
    }

    /** Retorna true si con esta devolucion el prestamo quedo FINALIZADO. */
    public boolean devolverEquipo(int detalleId, CondicionDevolucion condicion, String observacion) {
        return service.devolverEquipo(detalleId, condicion, observacion);
    }
}