package com.umg.sigelab.service;

import com.umg.sigelab.exception.BusinessException;
import com.umg.sigelab.model.AccionAuditoria;
import com.umg.sigelab.model.EstadoUsuario;
import com.umg.sigelab.model.NombreRol;
import com.umg.sigelab.model.Rol;
import com.umg.sigelab.model.Usuario;
import com.umg.sigelab.repository.AuditoriaRepository;
import com.umg.sigelab.repository.RolRepository;
import com.umg.sigelab.repository.UsuarioRepository;
import com.umg.sigelab.util.PasswordUtil;
import com.umg.sigelab.util.SesionActual;
import com.umg.sigelab.util.Transaccion;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Reglas de negocio de la gestion de usuarios.
 *  - Solo el ADMIN puede gestionar usuarios (RN-20: se valida aqui, no solo en el menu).
 *  - Campos obligatorios, formato de correo y de usuario, contrasena minima.
 *  - Usuario y correo unicos.
 *  - Desactivacion logica: se conserva el historial (6.2).
 *  - Un administrador no puede desactivarse ni cambiarse el rol a si mismo.
 *  - Cada operacion y su auditoria se guardan en la MISMA transaccion (RN-18).
 */
public class UsuarioService {

    private static final Pattern PATRON_USERNAME = Pattern.compile("^[A-Za-z0-9._-]{3,40}$");
    private static final Pattern PATRON_CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int PASSWORD_MINIMA = 8;

    private final UsuarioRepository usuarioRepository = new UsuarioRepository();
    private final RolRepository rolRepository = new RolRepository();
    private final AuditoriaRepository auditoriaRepository = new AuditoriaRepository();

    public List<Usuario> listar() {
        AutorizacionService.exigirRol(NombreRol.ADMIN);
        return usuarioRepository.listarTodos();
    }

    public List<Rol> listarRoles() {
        AutorizacionService.exigirRol(NombreRol.ADMIN);
        return rolRepository.listar();
    }

    /** Crea un usuario ACTIVO. 'datos' trae nombre, apellido, username, correo y rol. */
    public void crear(Usuario datos, String password) {
        AutorizacionService.exigirRol(NombreRol.ADMIN);

        String nombre = texto(datos.getNombre());
        String apellido = texto(datos.getApellido());
        String username = texto(datos.getUsername());
        String correo = texto(datos.getCorreo());
        validarDatosPersonales(nombre, apellido, correo, datos.getRol());

        if (!PATRON_USERNAME.matcher(username).matches()) {
            throw new BusinessException("El nombre de usuario debe tener entre 3 y 40 caracteres y solo "
                    + "puede contener letras, números, punto, guion y guion bajo.");
        }
        validarPassword(password);

        if (usuarioRepository.existeUsername(username, 0)) {
            throw new BusinessException("Ya existe un usuario con el nombre de usuario " + username + ".");
        }
        if (usuarioRepository.existeCorreo(correo, 0)) {
            throw new BusinessException("Ya existe un usuario con el correo " + correo + ".");
        }

        Usuario nuevo = new Usuario(0, nombre, apellido, username, PasswordUtil.hash(password),
                correo, datos.getRol(), EstadoUsuario.ACTIVO);
        int actor = SesionActual.getUsuario().getId();

        Transaccion.ejecutarSinResultado(con -> {
            int id = usuarioRepository.insertar(con, nuevo);
            auditoriaRepository.registrar(con, actor, AccionAuditoria.USUARIO_CREADO, "Usuario", id,
                    "Usuario creado: " + username + " (rol " + datos.getRol().getNombre() + ")");
        });
    }

    /**
     * Actualiza nombre, apellido, correo y rol. El nombre de usuario no se modifica.
     * Si 'nuevaPassword' viene vacia, la contrasena actual no cambia.
     */
    public void actualizar(Usuario datos, String nuevaPassword) {
        AutorizacionService.exigirRol(NombreRol.ADMIN);

        String nombre = texto(datos.getNombre());
        String apellido = texto(datos.getApellido());
        String correo = texto(datos.getCorreo());
        validarDatosPersonales(nombre, apellido, correo, datos.getRol());

        Usuario actual = buscarExistente(datos.getId());
        Usuario sesion = SesionActual.getUsuario();

        boolean esElMismo = actual.getId() == sesion.getId();
        if (esElMismo && actual.getRol().getId() != datos.getRol().getId()) {
            throw new BusinessException("No puede cambiar su propio rol.");
        }
        if (usuarioRepository.existeCorreo(correo, actual.getId())) {
            throw new BusinessException("Ya existe un usuario con el correo " + correo + ".");
        }

        boolean cambiaPassword = nuevaPassword != null && !nuevaPassword.isEmpty();
        if (cambiaPassword) {
            validarPassword(nuevaPassword);
        }

        Usuario cambios = new Usuario(actual.getId(), nombre, apellido, actual.getUsername(),
                actual.getPasswordHash(), correo, datos.getRol(), actual.getEstado());
        String hashNuevo = cambiaPassword ? PasswordUtil.hash(nuevaPassword) : null;

        Transaccion.ejecutarSinResultado(con -> {
            usuarioRepository.actualizar(con, cambios);
            if (hashNuevo != null) {
                usuarioRepository.actualizarPassword(con, cambios.getId(), hashNuevo);
            }
            auditoriaRepository.registrar(con, sesion.getId(), AccionAuditoria.USUARIO_ACTUALIZADO, "Usuario",
                    cambios.getId(), "Usuario actualizado: " + cambios.getUsername()
                            + (hashNuevo != null ? " (contraseña modificada)" : ""));
        });

        if (esElMismo) {
            SesionActual.iniciar(cambios); // mantiene la sesion coherente con los datos nuevos
        }
    }

    /** Desactivacion logica: el usuario ya no puede iniciar sesion, pero se conserva su historial. */
    public void desactivar(int idUsuario) {
        cambiarEstado(idUsuario, EstadoUsuario.INACTIVO, AccionAuditoria.USUARIO_DESACTIVADO);
    }

    public void reactivar(int idUsuario) {
        cambiarEstado(idUsuario, EstadoUsuario.ACTIVO, AccionAuditoria.USUARIO_REACTIVADO);
    }

    // ---------------- privados ----------------

    private void cambiarEstado(int idUsuario, EstadoUsuario nuevoEstado, AccionAuditoria accion) {
        AutorizacionService.exigirRol(NombreRol.ADMIN);

        Usuario usuario = buscarExistente(idUsuario);
        Usuario sesion = SesionActual.getUsuario();

        if (nuevoEstado == EstadoUsuario.INACTIVO && usuario.getId() == sesion.getId()) {
            throw new BusinessException("No puede desactivar su propia cuenta.");
        }
        if (usuario.getEstado() == nuevoEstado) {
            throw new BusinessException("El usuario " + usuario.getUsername() + " ya está "
                    + (nuevoEstado == EstadoUsuario.INACTIVO ? "inactivo." : "activo."));
        }

        Transaccion.ejecutarSinResultado(con -> {
            usuarioRepository.cambiarEstado(con, usuario.getId(), nuevoEstado);
            auditoriaRepository.registrar(con, sesion.getId(), accion, "Usuario", usuario.getId(),
                    (nuevoEstado == EstadoUsuario.INACTIVO ? "Usuario desactivado: " : "Usuario reactivado: ")
                            + usuario.getUsername());
        });
    }

    private Usuario buscarExistente(int id) {
        return usuarioRepository.buscarPorId(id)
                .orElseThrow(() -> new BusinessException("El usuario seleccionado ya no existe."));
    }

    private void validarDatosPersonales(String nombre, String apellido, String correo, Rol rol) {
        if (nombre.isEmpty()) {
            throw new BusinessException("Ingrese el nombre del usuario.");
        }
        if (apellido.isEmpty()) {
            throw new BusinessException("Ingrese el apellido del usuario.");
        }
        if (nombre.length() > 60 || apellido.length() > 60) {
            throw new BusinessException("El nombre y el apellido no pueden superar los 60 caracteres.");
        }
        if (correo.isEmpty()) {
            throw new BusinessException("Ingrese el correo del usuario.");
        }
        if (correo.length() > 100 || !PATRON_CORREO.matcher(correo).matches()) {
            throw new BusinessException("Ingrese un correo válido, por ejemplo usuario@umg.edu.gt.");
        }
        if (rol == null) {
            throw new BusinessException("Seleccione un rol para el usuario.");
        }
    }

    private void validarPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new BusinessException("Ingrese una contraseña.");
        }
        boolean tieneLetra = password.chars().anyMatch(Character::isLetter);
        boolean tieneNumero = password.chars().anyMatch(Character::isDigit);
        if (password.length() < PASSWORD_MINIMA || !tieneLetra || !tieneNumero) {
            throw new BusinessException("La contraseña debe tener al menos " + PASSWORD_MINIMA
                    + " caracteres e incluir letras y números.");
        }
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
