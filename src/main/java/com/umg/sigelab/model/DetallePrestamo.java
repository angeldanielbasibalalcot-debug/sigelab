package com.umg.sigelab.model;

import java.util.Date;

public class DetallePrestamo {

    private int id;

    private int prestamoId;

    private int equipoId;

    private Date fechaDevolucion;

    private String condicionDevolucion;

    private String observacionDevolucion;

    private Integer usuarioRecibeId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPrestamoId() {
        return prestamoId;
    }

    public void setPrestamoId(int prestamoId) {
        this.prestamoId = prestamoId;
    }

    public int getEquipoId() {
        return equipoId;
    }

    public void setEquipoId(int equipoId) {
        this.equipoId = equipoId;
    }

    public Date getFechaDevolucion() {
        return fechaDevolucion;
    }

    public void setFechaDevolucion(Date fechaDevolucion) {
        this.fechaDevolucion = fechaDevolucion;
    }

    public String getCondicionDevolucion() {
        return condicionDevolucion;
    }

    public void setCondicionDevolucion(String condicionDevolucion) {
        this.condicionDevolucion = condicionDevolucion;
    }

    public String getObservacionDevolucion() {
        return observacionDevolucion;
    }

    public void setObservacionDevolucion(String observacionDevolucion) {
        this.observacionDevolucion = observacionDevolucion;
    }

    public Integer getUsuarioRecibeId() {
        return usuarioRecibeId;
    }

    public void setUsuarioRecibeId(Integer usuarioRecibeId) {
        this.usuarioRecibeId = usuarioRecibeId;
    }
}



