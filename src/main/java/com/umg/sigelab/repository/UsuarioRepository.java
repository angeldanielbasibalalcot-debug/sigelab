package com.umg.sigelab.repository;

import com.umg.sigelab.exception.DataAccessException;
import com.umg.sigelab.model.EstadoUsuario;
import com.umg.sigelab.model.NombreRol;
import com.umg.sigelab.model.Rol;
import com.umg.sigelab.model.Usuario;
import com.umg.sigelab.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de Usuario. Aqui vive el SQL; no hay reglas de negocio.
 * Las consultas abren su propia conexion. Las escrituras reciben la Connection
 * para poder ejecutarse dentro de una transaccion (ver util.Transaccion).
 */
public class UsuarioRepository {

    private static final String SELECT_BASE =
            "SELECT u.id, u.nombre, u.apellido, u.username, u.password_hash, u.correo, u.estado, "
                    + "r.id AS rol_id, r.nombre AS rol_nombre "
                    + "FROM Usuario u JOIN Rol r ON r.id = u.rol_id ";

    // ---------------- Consultas ----------------

    public Optional<Usuario> buscarPorUsername(String username) {
        return buscarUno(SELECT_BASE + "WHERE u.username = ?", username);
    }

    public Optional<Usuario> buscarPorId(int id) {
        return buscarUno(SELECT_BASE + "WHERE u.id = ?", id);
    }

    public List<Usuario> listarTodos() {
        String sql = SELECT_BASE + "ORDER BY u.id";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Usuario> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar la lista de usuarios.", e);
        }
    }

    /** excluirId = 0 para no excluir a nadie (al crear). Al editar se excluye el propio registro. */
    public boolean existeUsername(String username, int excluirId) {
        return existe("SELECT COUNT(*) FROM Usuario WHERE username = ? AND id <> ?", username, excluirId);
    }

    public boolean existeCorreo(String correo, int excluirId) {
        return existe("SELECT COUNT(*) FROM Usuario WHERE correo = ? AND id <> ?", correo, excluirId);
    }

    // ---------------- Escrituras (con Connection) ----------------

    /** Inserta el usuario y devuelve el id generado. */
    public int insertar(Connection con, Usuario u) {
        String sql = "INSERT INTO Usuario (nombre, apellido, username, password_hash, correo, rol_id, estado) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getNombre());
            ps.setString(2, u.getApellido());
            ps.setString(3, u.getUsername());
            ps.setString(4, u.getPasswordHash());
            ps.setString(5, u.getCorreo());
            ps.setInt(6, u.getRol().getId());
            ps.setString(7, u.getEstado().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
                throw new DataAccessException("No se pudo obtener el id del usuario creado.");
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo guardar el usuario.", e);
        }
    }

    /** Actualiza datos personales y rol. No toca username, contrasena ni estado. */
    public void actualizar(Connection con, Usuario u) {
        String sql = "UPDATE Usuario SET nombre = ?, apellido = ?, correo = ?, rol_id = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, u.getNombre());
            ps.setString(2, u.getApellido());
            ps.setString(3, u.getCorreo());
            ps.setInt(4, u.getRol().getId());
            ps.setInt(5, u.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar el usuario.", e);
        }
    }

    public void actualizarPassword(Connection con, int id, String passwordHash) {
        String sql = "UPDATE Usuario SET password_hash = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar la contraseña.", e);
        }
    }

    public void cambiarEstado(Connection con, int id, EstadoUsuario estado) {
        String sql = "UPDATE Usuario SET estado = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado.name());
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo cambiar el estado del usuario.", e);
        }
    }

    // ---------------- Auxiliares ----------------

    private Optional<Usuario> buscarUno(String sql, Object parametro) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar el usuario.", e);
        }
    }

    private boolean existe(String sql, String valor, int excluirId) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, valor);
            ps.setInt(2, excluirId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo validar la información del usuario.", e);
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Rol rol = new Rol(rs.getInt("rol_id"), NombreRol.valueOf(rs.getString("rol_nombre")));
        return new Usuario(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getString("apellido"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("correo"),
                rol,
                EstadoUsuario.valueOf(rs.getString("estado")));
    }
}
