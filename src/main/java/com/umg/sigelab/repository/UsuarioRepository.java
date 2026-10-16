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
import java.util.Optional;

/** Acceso a datos de Usuario. Aqui vive el SQL; no hay reglas de negocio. */
public class UsuarioRepository {

    private static final String SELECT_BASE =
            "SELECT u.id, u.nombre, u.apellido, u.username, u.password_hash, u.correo, u.estado, "
                    + "r.id AS rol_id, r.nombre AS rol_nombre "
                    + "FROM Usuario u JOIN Rol r ON r.id = u.rol_id ";

    public Optional<Usuario> buscarPorUsername(String username) {
        String sql = SELECT_BASE + "WHERE u.username = ?";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar el usuario.", e);
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
