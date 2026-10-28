package com.umg.sigelab.repository;

import com.umg.sigelab.exception.DataAccessException;
import com.umg.sigelab.model.AccionAuditoria;
import com.umg.sigelab.model.RegistroAuditoria;
import com.umg.sigelab.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Registra y consulta la tabla Auditoria.
 *
 * IMPORTANTE para el equipo: dentro de una transaccion (prestamo, devolucion, mantenimiento)
 * usen la version que recibe la Connection, para que la auditoria quede en la misma
 * transaccion que el resto de operaciones.
 */
public class AuditoriaRepository {

    private static final String INSERT =
            "INSERT INTO Auditoria (usuario_id, accion, entidad, id_registro, descripcion) "
                    + "VALUES (?, ?, ?, ?, ?)";

    private static final String CONSULTA_BASE = """
            SELECT
                a.id,
                a.usuario_id,
                u.username,
                a.fecha_hora,
                a.accion,
                a.entidad,
                a.id_registro,
                a.descripcion
            FROM Auditoria a
            LEFT JOIN Usuario u ON u.id = a.usuario_id
            """;

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

    // Consulta con filtros opcionales: accion (null = todas) y rango de
    // fechas (desde/hasta inclusive; null = sin limite). Usa BETWEEN.
    public List<RegistroAuditoria> consultar(String accion,
                                             LocalDate desde,
                                             LocalDate hasta) {
        StringBuilder sql = new StringBuilder(CONSULTA_BASE);
        List<Object> parametros = new ArrayList<>();
        sql.append(" WHERE 1 = 1");

        if (accion != null) {
            sql.append(" AND a.accion = ?");
            parametros.add(accion);
        }

        if (desde != null && hasta != null) {
            sql.append(" AND a.fecha_hora BETWEEN ? AND ?");
            parametros.add(Timestamp.valueOf(desde.atStartOfDay()));
            parametros.add(Timestamp.valueOf(hasta.atTime(23, 59, 59)));
        } else if (desde != null) {
            sql.append(" AND a.fecha_hora >= ?");
            parametros.add(Timestamp.valueOf(desde.atStartOfDay()));
        } else if (hasta != null) {
            sql.append(" AND a.fecha_hora <= ?");
            parametros.add(Timestamp.valueOf(hasta.atTime(23, 59, 59)));
        }

        sql.append(" ORDER BY a.fecha_hora DESC, a.id DESC");

        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < parametros.size(); i++) {
                Object p = parametros.get(i);
                if (p instanceof Timestamp t) {
                    ps.setTimestamp(i + 1, t);
                } else {
                    ps.setString(i + 1, (String) p);
                }
            }

            try (ResultSet rs = ps.executeQuery()) {
                List<RegistroAuditoria> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
                return lista;
            }

        } catch (SQLException e) {
            throw new DataAccessException(
                    "No se pudo consultar la auditoria.", e);
        }
    }

    private RegistroAuditoria mapear(ResultSet rs) throws SQLException {
        RegistroAuditoria r = new RegistroAuditoria();
        r.setId(rs.getLong("id"));

        int usuarioId = rs.getInt("usuario_id");
        r.setUsuarioId(rs.wasNull() ? null : usuarioId);

        r.setNombreUsuario(rs.getString("username"));
        r.setFechaHora(rs.getTimestamp("fecha_hora").toLocalDateTime());
        r.setAccion(rs.getString("accion"));
        r.setEntidad(rs.getString("entidad"));

        int idRegistro = rs.getInt("id_registro");
        r.setIdRegistro(rs.wasNull() ? null : idRegistro);

        r.setDescripcion(rs.getString("descripcion"));
        return r;
    }
}