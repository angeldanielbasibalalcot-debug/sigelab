package com.umg.sigelab.controller;

import com.umg.sigelab.model.Usuario;
import com.umg.sigelab.service.AuthService;

/** Coordina el LoginFrame con AuthService. No contiene reglas de negocio. */
public class AuthController {

    private final AuthService authService = new AuthService();

    public Usuario validarCredenciales(String username, String password) {
        return authService.autenticar(username, password);
    }

    public void validarRostro(Usuario usuario) {
        authService.verificarRostro(usuario);
    }

    public void confirmarAcceso(Usuario usuario) {
        authService.confirmarAcceso(usuario);
    }
}
