package com.umg.sigelab.service;

import com.umg.sigelab.exception.BusinessException;
import com.umg.sigelab.model.AccionAuditoria;
import com.umg.sigelab.model.Usuario;
import com.umg.sigelab.repository.AuditoriaRepository;
import com.umg.sigelab.repository.UsuarioRepository;
import com.umg.sigelab.util.PasswordUtil;
import com.umg.sigelab.util.SesionActual;

import java.util.Optional;

/**
 * Reglas de negocio del inicio de sesion:
 *  RN-01: un usuario INACTIVO no puede iniciar sesion.
 *  RN-02: credenciales validas no bastan; la validacion facial debe aprobarse.
 *  RN-18: el login exitoso y el fallido quedan en Auditoria.
 *
 * Flujo: autenticar() -> verificarRostro() -> confirmarAcceso()
 */
public class AuthService {

    private static final String MSG_CREDENCIALES = "Usuario o contraseña incorrectos.";

    private final UsuarioRepository usuarioRepository = new UsuarioRepository();
    private final AuditoriaRepository auditoriaRepository = new AuditoriaRepository();
    private final FacialService facialService = new FacialService();

    /** Paso 1: valida usuario, contraseña y estado. No inicia la sesion todavia. */
    public Usuario autenticar(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new BusinessException("Ingrese su usuario y su contraseña.");
        }
        String user = username.trim();

        Optional<Usuario> encontrado = usuarioRepository.buscarPorUsername(user);
        if (encontrado.isEmpty()) {
            auditoriaRepository.registrar(null, AccionAuditoria.LOGIN_FALLIDO, "Usuario", null,
                    "Intento de login con usuario inexistente: " + recortar(user, 50));
            throw new BusinessException(MSG_CREDENCIALES);
        }

        Usuario usuario = encontrado.get();
        if (!PasswordUtil.coincide(password, usuario.getPasswordHash())) {
            auditoriaRepository.registrar(usuario.getId(), AccionAuditoria.LOGIN_FALLIDO, "Usuario",
                    usuario.getId(), "Contraseña incorrecta para el usuario " + usuario.getUsername());
            throw new BusinessException(MSG_CREDENCIALES);
        }

        // RN-01
        if (!usuario.estaActivo()) {
            auditoriaRepository.registrar(usuario.getId(), AccionAuditoria.LOGIN_FALLIDO, "Usuario",
                    usuario.getId(), "Intento de login con usuario INACTIVO: " + usuario.getUsername());
            throw new BusinessException("El usuario está inactivo. Comuníquese con el administrador.");
        }
        return usuario;
    }

    /** Paso 2 (RN-02): validacion facial. Si falla, se deniega el acceso. */
    public void verificarRostro(Usuario usuario) {
        if (!facialService.verificar(usuario)) {
            auditoriaRepository.registrar(usuario.getId(), AccionAuditoria.LOGIN_FALLIDO, "Usuario",
                    usuario.getId(), "Validacion facial rechazada para " + usuario.getUsername());
            throw new BusinessException("No se pudo validar su rostro. Acceso denegado.");
        }
    }

    /** Paso 3: abre la sesion y deja rastro en Auditoria. */
    public void confirmarAcceso(Usuario usuario) {
        SesionActual.iniciar(usuario);
        auditoriaRepository.registrar(usuario.getId(), AccionAuditoria.LOGIN_EXITOSO, "Usuario",
                usuario.getId(), "Inicio de sesion de " + usuario.getUsername());
    }

    private String recortar(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max);
    }
}
