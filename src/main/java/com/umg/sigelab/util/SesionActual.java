package com.umg.sigelab.util;

import com.umg.sigelab.model.Usuario;

/** Guarda el usuario que inicio sesion. Se usa para auditoria y para validar permisos. */
public final class SesionActual {

    private static Usuario usuario;

    private SesionActual() {
    }

    public static synchronized void iniciar(Usuario u) {
        usuario = u;
    }

    public static synchronized void cerrar() {
        usuario = null;
    }

    public static synchronized Usuario getUsuario() {
        return usuario;
    }

    public static synchronized boolean estaActiva() {
        return usuario != null;
    }
}
