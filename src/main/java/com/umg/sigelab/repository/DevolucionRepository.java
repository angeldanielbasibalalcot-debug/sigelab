package com.umg.sigelab.repository;

import com.umg.sigelab.exception.DataAccessException;
import com.umg.sigelab.model.EquipoPrestado;
import com.umg.sigelab.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de las devoluciones. Solo SQL, sin reglas de negocio.
 * La devolucion vive en las columnas de DetallePrestamo (no hay tabla Devolucion).
 * Los metodos que reciben Connection se usan dentro de una Transaccion del Service.
 */
public class DevolucionRepository {

    private static final String SELECT_BASE =
            "SELECT dp.id AS detalle_id, p.id AS prestamo_id, e.id AS equipo_id, "
                    + "p.solicitante, e.codigo_inventario, e.nombre, p.fecha_esperada_devolucion, "
                    + "dp.fecha_devolucion, p.estado AS estado_prestamo "
                    + "FROM DetallePrestamo dp "
                    + "INNER JOIN Prestamo p ON dp.prestamo_id = p.id "
                    + "INNER JOIN Equipo e ON dp.equipo_id = e.id ";

    /** Equipos pendientes de devolucion dentro de prestamos ACTIVOS (RN-10). */
    public List<EquipoPrestado> listarPendientes() {
        String sql = SELECT_BASE
                + "WHERE dp.fecha_devolucion IS NULL AND p.estado = 'ACTIVO' "
                + "ORDER BY p.id, e.codigo_inventario";
        List<EquipoPrestado> lista = new ArrayList<>();
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "No se pudo consultar los equipos pendientes de devoluci\u00f3n.", e);
        }
        return lista;
    }

    /** Busca una linea de detalle por id (devuelta o no). Devuelve null si no existe. */
    public EquipoPrestado obtenerDetalle(Connection con, int detalleId) throws SQLException {
        String sql = SELECT_BASE + "WHERE dp.id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, detalleId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /**
     * Marca la devolucion de un detalle. El filtro fecha_devolucion IS NULL evita
     * devolver dos veces (RN-11). Devuelve las filas afectadas (1 si se pudo).
     */
    public int registrarDevolucion(Connection con, int detalleId, String condicion,
                                   String observacion, int usuarioRecibeId,
                                   LocalDateTime fecha) throws SQLException {
        String sql = "UPDATE DetallePrestamo SET fecha_devolucion = ?, "
                + "condicion_devolucion = ?, observacion_devolucion = ?, usuario_recibe_id = ? "
                + "WHERE id = ? AND fecha_devolucion IS NULL";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(fecha));
            ps.setString(2, condicion);
            ps.setString(3, observacion);
            ps.setInt(4, usuarioRecibeId);
            ps.setInt(5, detalleId);
            return ps.executeUpdate();
        }
    }

    /**
     * Cambia el estado del equipo solo si sigue PRESTADO (RN-12, RN-13).
     * Devuelve las filas afectadas (1 si se pudo).
     */
    public int actualizarEstadoEquipoPrestado(Connection con, int equipoId,
                                              String nuevoEstado) throws SQLException {
        String sql = "UPDATE Equipo SET estado = ? WHERE id = ? AND estado = 'PRESTADO'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setInt(2, equipoId);
            return ps.executeUpdate();
        }
    }

    /** Cuantos equipos de un prestamo siguen sin devolver (RN-16). */
    public int contarPendientes(Connection con, int prestamoId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM DetallePrestamo "
                + "WHERE prestamo_id = ? AND fecha_devolucion IS NULL";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, prestamoId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Pasa el prestamo a FINALIZADO cuando ya no quedan pendientes (RN-16). */
    public int finalizarPrestamo(Connection con, int prestamoId) throws SQLException {
        String sql = "UPDATE Prestamo SET estado = 'FINALIZADO' WHERE id = ? AND estado = 'ACTIVO'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, prestamoId);
            return ps.executeUpdate();
        }
    }

    private EquipoPrestado mapear(ResultSet rs) throws SQLException {
        Timestamp esperada = rs.getTimestamp("fecha_esperada_devolucion");
        return new EquipoPrestado(
                rs.getInt("detalle_id"),
                rs.getInt("prestamo_id"),
                rs.getInt("equipo_id"),
                rs.getString("solicitante"),
                rs.getString("codigo_inventario"),
                rs.getString("nombre"),
                esperada == null ? null : esperada.toLocalDateTime(),
                rs.getTimestamp("fecha_devolucion") != null,
                rs.getString("estado_prestamo"));
    }
}