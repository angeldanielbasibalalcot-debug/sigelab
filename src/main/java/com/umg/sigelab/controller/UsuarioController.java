package com.umg.sigelab.controller;

import com.umg.sigelab.model.Rol;
import com.umg.sigelab.model.Usuario;
import com.umg.sigelab.service.UsuarioService;

import java.util.List;

/** Coordina UsuariosPanel con UsuarioService. No contiene reglas de negocio. */
public class UsuarioController {

    private final UsuarioService usuarioService = new UsuarioService();

    public List<Usuario> listar() {
        return usuarioService.listar();
    }

    public List<Rol> listarRoles() {
        return usuarioService.listarRoles();
    }

    public void crear(Usuario datos, String password) {
        usuarioService.crear(datos, password);
    }

    public void actualizar(Usuario datos, String nuevaPassword) {
        usuarioService.actualizar(datos, nuevaPassword);
    }

    public void desactivar(int idUsuario) {
        usuarioService.desactivar(idUsuario);
    }

    public void reactivar(int idUsuario) {
        usuarioService.reactivar(idUsuario);
    }
}
