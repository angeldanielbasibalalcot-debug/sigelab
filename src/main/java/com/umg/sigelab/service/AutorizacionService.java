package com.umg.sigelab.service;

import com.umg.sigelab.exception.BusinessException;
import com.umg.sigelab.model.NombreRol;
import com.umg.sigelab.model.Usuario;
import com.umg.sigelab.util.SesionActual;

/**
 * Validacion de permisos en la logica de negocio (RN-20).
 * Ocultar un menu NO es suficiente: cada Service debe llamar a exigirRol(...)
 * al inicio de las operaciones sensibles.
 *
 * Ejemplo en un Service:
 *     AutorizacionService.exigirRol(NombreRol.ADMIN, NombreRol.ENCARGADO);
 */
public final class AutorizacionService {

    private AutorizacionService() {
    }

    public static void exigirRol(NombreRol... permitidos) {
        Usuario u = SesionActual.getUsuario();
        if (u == null) {
            throw new BusinessException("Debe iniciar sesión para realizar esta operación.");
        }
        if (!u.tieneRol(permitidos)) {
            throw new BusinessException("No tiene permisos para realizar esta operación.");
        }
    }
}
