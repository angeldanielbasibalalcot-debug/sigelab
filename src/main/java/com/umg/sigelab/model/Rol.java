package com.umg.sigelab.model;

public class Rol {

    private int id;
    private NombreRol nombre;

    public Rol() {
    }

    public Rol(int id, NombreRol nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public NombreRol getNombre() {
        return nombre;
    }

    public void setNombre(NombreRol nombre) {
        this.nombre = nombre;
    }

    @Override
    public String toString() {
        return nombre.name();
    }
}
