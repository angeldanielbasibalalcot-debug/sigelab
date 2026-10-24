package com.umg.sigelab.model;

public class TipoEquipo {

    private int id;
    private String nombre;
    private String descripcion; // opcional: en la BD puede ser NULL

    public TipoEquipo() {
    }

    public TipoEquipo(int id, String nombre, String descripcion) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    // Para que un JComboBox muestre el nombre ("Laptop", "Cámara"...)
    @Override
    public String toString() {
        return nombre;
    }
}