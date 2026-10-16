package com.umg.sigelab.repository;

import com.umg.sigelab.exception.DataAccessException;
import com.umg.sigelab.model.AccionAuditoria;
import com.umg.sigelab.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Inserta registros en la tabla Auditoria.
 *
 * IMPORTANTE para el equipo: dentro de una transaccion (prestamo, devolucion, mantenimiento)
 * usen la version que recibe la Connection, para que la auditoria quede en la misma
 * transaccion que el resto de operaciones.
 */
public class AuditoriaRepository {

    private static final String INSERT =
            "INSERT INTO Auditoria (usuario_id, accion, entidad, id_registro, descripcion) "
                    + "VALUES (?, ?, ?, ?, ?)";

    /** Registra usando su propia conexion (auto-commit). */
    public void registrar(Integer usuarioId, AccionAuditoria accion, String entidad,
                          Integer idRegistro, String descripcion) {
        try (Connection con = ConexionBD.obtenerConexion()) {
            registrar(con, usuarioId, accion, entidad, idRegistro, descripcion);
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo registrar la auditoria.", e);
        }
    }

    /** Registra usando una conexion existente (para transacciones). No la cierra. */
    public void registrar(Connection con, Integer usuarioId, AccionAuditoria accion, String entidad,
                          Integer idRegistro, String descripcion) {
        try (PreparedStatement ps = con.prepareStatement(INSERT)) {
            if (usuarioId == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, usuarioId);
            }
            ps.setString(2, accion.name());
            ps.setString(3, entidad);
            if (idRegistro == null) {
                ps.setNull(4, Types.INTEGER);
            } else {
                ps.setInt(4, idRegistro);
            }
            ps.setString(5, descripcion);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo registrar la auditoria.", e);
        }
    }
}
