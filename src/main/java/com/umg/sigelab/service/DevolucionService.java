package com.umg.sigelab.service;

import com.umg.sigelab.repository.DevolucionRepository;

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
}