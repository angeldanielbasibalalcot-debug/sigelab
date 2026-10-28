package com.umg.sigelab.service;

import com.umg.sigelab.repository.DevolucionRepository;
import com.umg.sigelab.model.DetallePrestamoCombo;
import java.util.List;

public class DevolucionService {

    private DevolucionRepository repository =
            new DevolucionRepository();

    public void devolverEquipo(
            int detallePrestamoId,
            String condicion,
            String observacion,
            int usuarioRecibeId) {

        repository.devolverEquipo(
                detallePrestamoId,
                condicion,
                observacion,
                usuarioRecibeId);
    }
    public void actualizarEstadoDisponible(
            int detallePrestamoId) {

        repository.actualizarEstadoDisponible(
                detallePrestamoId);

    }
    public void finalizarPrestamo(
            int detallePrestamoId) {

        repository.finalizarPrestamo(
                detallePrestamoId);

    }
    public List<DetallePrestamoCombo>
    listarPendientes() {

        return repository.listarPendientes();

    }
}