package com.umg.sigelab.model;

public class DetallePrestamoCombo {

    private int detalleId;

    private String descripcion;

    public DetallePrestamoCombo(
            int detalleId,
            String descripcion) {

        this.detalleId = detalleId;
        this.descripcion = descripcion;
    }

    public int getDetalleId() {
        return detalleId;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
