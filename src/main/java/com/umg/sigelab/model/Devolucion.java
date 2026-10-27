package com.umg.sigelab.model;

import java.util.Date;

public class Devolucion {

    private int id;

    private int detallePrestamoId;

    private Date fechaDevolucion;

    private CondicionDevolucion condicion;

    private String observaciones;

    private int usuarioRecibeId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDetallePrestamoId() {
        return detallePrestamoId;
    }

    public void setDetallePrestamoId(int detallePrestamoId) {
        this.detallePrestamoId = detallePrestamoId;
    }

    public Date getFechaDevolucion() {
        return fechaDevolucion;
    }

    public void setFechaDevolucion(Date fechaDevolucion) {
        this.fechaDevolucion = fechaDevolucion;
    }

    public CondicionDevolucion getCondicion() {
        return condicion;
    }

    public void setCondicion(
            CondicionDevolucion condicion) {

        this.condicion = condicion;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(
            String observaciones) {

        this.observaciones = observaciones;
    }

    public int getUsuarioRecibeId() {
        return usuarioRecibeId;
    }

    public void setUsuarioRecibeId(
            int usuarioRecibeId) {

        this.usuarioRecibeId = usuarioRecibeId;
    }
}
