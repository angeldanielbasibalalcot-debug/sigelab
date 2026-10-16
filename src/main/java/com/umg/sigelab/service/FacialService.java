package com.umg.sigelab.service;

import com.umg.sigelab.model.Usuario;

/**
 * Segundo factor del login (seccion 6.1).
 *
 * *** TEMPORAL ***
 * Mientras no exista el componente facial (Python + OpenCV), este metodo aprueba siempre.
 * Cuando se integre la API local, aqui se hara la llamada HTTP (HttpClient) y se
 * devolvera true solo si la API confirma que el rostro corresponde al usuario.
 * Esta clase DEBE reemplazarse antes de la entrega para cumplir RN-02 y CP-02.
 */
public class FacialService {

    public boolean verificar(Usuario usuario) {
        return true;
    }
}
