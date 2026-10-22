package com.umg.sigelab.model;

public class Laboratorio {

    private int id;
    private String codigo;
    private String nombre;
    private String ubicacion;
    private int capacidad;
    private EstadoLaboratorio estado;

    public Laboratorio() {
    }

    public Laboratorio(int id, String codigo, String nombre, String ubicacion,
                       int capacidad, EstadoLaboratorio estado) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.ubicacion = ubicacion;
        this.capacidad = capacidad;
        this.estado = estado;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(int capacidad) {
        this.capacidad = capacidad;
    }

    public EstadoLaboratorio getEstado() {
        return estado;
    }

    public void setEstado(EstadoLaboratorio estado) {
        this.estado = estado;
    }

    // Se usa para que un JComboBox muestre "LAB-01 - Laboratorio de Redes"
    @Override
    public String toString() {
        return codigo + " - " + nombre;
    }
}