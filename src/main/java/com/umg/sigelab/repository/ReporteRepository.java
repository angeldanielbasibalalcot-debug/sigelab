package com.umg.sigelab.repository;

import com.umg.sigelab.exception.DataAccessException;
import com.umg.sigelab.model.ConteoEstado;
import com.umg.sigelab.model.FilaHistorialEquipo;
import com.umg.sigelab.model.FilaHistorialMantenimiento;
import com.umg.sigelab.model.FilaPrestamo;
import com.umg.sigelab.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Consultas de solo lectura para el dashboard y los reportes (6.11).
 * Solo contiene SQL: el calculo de "vencido" (RN-17) lo hace el Service.
 */
public class ReporteRepository {

    private static final String EQUIPOS_POR_ESTADO = """
            SELECT estado, COUNT(*) AS total
            FROM Equipo
            GROUP BY estado
            ORDER BY estado
            """;

    private static final String PRESTAMOS_BASE = """
            SELECT
                p.id,
                p.solicitante,
                p.fecha_prestamo,
                p.fecha_esperada_devolucion,
                p.estado,
                u.username,
                COUNT(d.id) AS total_equipos,
                SUM(CASE WHEN d.fecha_devolucion IS NULL THEN 1 ELSE 0 END) AS pendientes
            FROM Prestamo p
            JOIN Usuario u ON u.id = p.usuario_registra_id
            JOIN DetallePrestamo d ON d.prestamo_id = p.id
            """;

    private static final String PRESTAMOS_AGRUPAR = """
             GROUP BY p.id, p.solicitante, p.fecha_prestamo, p.fecha_esperada_devolucion,
                      p.estado, u.username
            """;

    private static final String HISTORIAL_EQUIPO = """
            SELECT
                p.id,
                p.solicitante,
                p.fecha_prestamo,
                p.fecha_esperada_devolucion,
                d.fecha_devolucion,
                d.condicion_devolucion,
                d.observacion_devolucion
            FROM DetallePrestamo d
            JOIN Prestamo p ON p.id = d.prestamo_id
            JOIN Equipo e ON e.id = d.equipo_id
            WHERE e.codigo_inventario = ?
            ORDER BY p.fecha_prestamo DESC, p.id DESC
            """;

    private static final String HISTORIAL_MANTENIMIENTO = """
            SELECT
                m.id,
                e.codigo_inventario,
                e.nombre,
                m.fecha_ingreso,
                m.tipo,
                m.descripcion,
                m.responsable,
                m.fecha_salida,
                m.resultado,
                m.estado
            FROM Mantenimiento m
            JOIN Equipo e ON e.id = m.equipo_id
            WHERE (? IS NULL OR e.codigo_inventario = ?)
            ORDER BY m.fecha_ingreso DESC, m.id DESC
            """;

    /** Equipos agrupados por estado (GROUP BY + COUNT). */
    public List<ConteoEstado> contarEquiposPorEstado() {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(EQUIPOS_POR_ESTADO);
             ResultSet rs = ps.executeQuery()) {
            List<ConteoEstado> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(new ConteoEstado(rs.getString("estado"), rs.getInt("total")));
            }
            return lista;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar los equipos por estado.", e);
        }
    }

    /** Cantidad de prestamos con estado ACTIVO. */
    public int contarPrestamosActivos() {
        String sql = "SELECT COUNT(*) FROM Prestamo WHERE estado = 'ACTIVO'";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo contar los prestamos activos.", e);
        }
    }

    /**
     * Todos los prestamos (sin filtrar por estado), mas recientes primero.
     * El Service los clasifica en activos, finalizados o vencidos.
     */
    public List<FilaPrestamo> listarPrestamos() {
        String sql = PRESTAMOS_BASE + PRESTAMOS_AGRUPAR
                + " ORDER BY p.fecha_prestamo DESC, p.id DESC";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return mapearPrestamos(rs);
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar los prestamos.", e);
        }
    }

    /** Prestamos cuya fecha de prestamo esta entre dos fechas (BETWEEN, inclusive). */
    public List<FilaPrestamo> listarPrestamosEntreFechas(LocalDate desde, LocalDate hasta) {
        String sql = PRESTAMOS_BASE
                + " WHERE p.fecha_prestamo BETWEEN ? AND ?"
                + PRESTAMOS_AGRUPAR
                + " ORDER BY p.fecha_prestamo DESC, p.id DESC";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(hasta.atTime(23, 59, 59)));
            try (ResultSet rs = ps.executeQuery()) {
                return mapearPrestamos(rs);
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar los prestamos entre fechas.", e);
        }
    }

    /** Historial de prestamos de un equipo, buscado por su codigo de inventario. */
    public List<FilaHistorialEquipo> historialDeEquipo(String codigoInventario) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(HISTORIAL_EQUIPO)) {
            ps.setString(1, codigoInventario);
            try (ResultSet rs = ps.executeQuery()) {
                List<FilaHistorialEquipo> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(new FilaHistorialEquipo(
                            rs.getInt("id"),
                            rs.getString("solicitante"),
                            rs.getTimestamp("fecha_prestamo").toLocalDateTime(),
                            rs.getTimestamp("fecha_esperada_devolucion").toLocalDateTime(),
                            fecha(rs.getTimestamp("fecha_devolucion")),
                            rs.getString("condicion_devolucion"),
                            rs.getString("observacion_devolucion")));
                }
                return lista;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar el historial del equipo.", e);
        }
    }

    /** Historial de mantenimiento; con codigo null devuelve el de todos los equipos. */
    public List<FilaHistorialMantenimiento> historialMantenimiento(String codigoInventario) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(HISTORIAL_MANTENIMIENTO)) {
            ps.setString(1, codigoInventario);
            ps.setString(2, codigoInventario);
            try (ResultSet rs = ps.executeQuery()) {
                List<FilaHistorialMantenimiento> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(new FilaHistorialMantenimiento(
                            rs.getInt("id"),
                            rs.getString("codigo_inventario"),
                            rs.getString("nombre"),
                            rs.getTimestamp("fecha_ingreso").toLocalDateTime(),
                            rs.getString("tipo"),
                            rs.getString("descripcion"),
                            rs.getString("responsable"),
                            fecha(rs.getTimestamp("fecha_salida")),
                            rs.getString("resultado"),
                            rs.getString("estado")));
                }
                return lista;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar el historial de mantenimiento.", e);
        }
    }

    private List<FilaPrestamo> mapearPrestamos(ResultSet rs) throws SQLException {
        List<FilaPrestamo> lista = new ArrayList<>();
        while (rs.next()) {
            String estado = rs.getString("estado");
            lista.add(new FilaPrestamo(
                    rs.getInt("id"),
                    rs.getString("solicitante"),
                    rs.getTimestamp("fecha_prestamo").toLocalDateTime(),
                    rs.getTimestamp("fecha_esperada_devolucion").toLocalDateTime(),
                    estado,
                    rs.getString("username"),
                    rs.getInt("total_equipos"),
                    rs.getInt("pendientes"),
                    estado));
        }
        return lista;
    }

    private LocalDateTime fecha(Timestamp t) {
        return t == null ? null : t.toLocalDateTime();
    }
}
