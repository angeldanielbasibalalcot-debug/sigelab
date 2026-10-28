package com.umg.sigelab.model;

import java.util.Date;

public class Prestamo {

    private int id;

    private String solicitante;

    private Date fechaPrestamo;

    private Date fechaEsperadaDevolucion;

    private int usuarioRegistraId;

    private EstadoPrestamo estado;

    private String observaciones;

    public Prestamo() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(String solicitante) {
        this.solicitante = solicitante;
    }

    public Date getFechaPrestamo() {
        return fechaPrestamo;
    }

    public void setFechaPrestamo(Date fechaPrestamo) {
        this.fechaPrestamo = fechaPrestamo;
    }

    public Date getFechaEsperadaDevolucion() {
        return fechaEsperadaDevolucion;
    }

    public void setFechaEsperadaDevolucion(Date fechaEsperadaDevolucion) {
        this.fechaEsperadaDevolucion = fechaEsperadaDevolucion;
    }

    public int getUsuarioRegistraId() {
        return usuarioRegistraId;
    }

    public void setUsuarioRegistraId(int usuarioRegistraId) {
        this.usuarioRegistraId = usuarioRegistraId;
    }

    public EstadoPrestamo getEstado() {
        return estado;
    }

    public void setEstado(EstadoPrestamo estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}