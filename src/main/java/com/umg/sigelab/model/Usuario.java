package com.umg.sigelab.model;

public class Usuario {

    private int id;
    private String nombre;
    private String apellido;
    private String username;
    private String passwordHash;
    private String correo;
    private Rol rol;
    private EstadoUsuario estado;

    public Usuario() {
    }

    public Usuario(int id, String nombre, String apellido, String username,
                   String passwordHash, String correo, Rol rol, EstadoUsuario estado) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.username = username;
        this.passwordHash = passwordHash;
        this.correo = correo;
        this.rol = rol;
        this.estado = estado;
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

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public EstadoUsuario getEstado() {
        return estado;
    }

    public void setEstado(EstadoUsuario estado) {
        this.estado = estado;
    }

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

    public boolean estaActivo() {
        return estado == EstadoUsuario.ACTIVO;
    }

    /** Devuelve true si el rol del usuario es alguno de los indicados. */
    public boolean tieneRol(NombreRol... roles) {
        if (rol == null) {
            return false;
        }
        for (NombreRol r : roles) {
            if (rol.getNombre() == r) {
                return true;
            }
        }
        return false;
    }
}
