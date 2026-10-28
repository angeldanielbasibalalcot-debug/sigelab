package com.umg.sigelab.repository;

import com.umg.sigelab.exception.DataAccessException;
import com.umg.sigelab.model.Equipo;
import com.umg.sigelab.model.Prestamo;
import com.umg.sigelab.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de prestamos. Solo SQL, sin reglas de negocio.
 * Los metodos que reciben Connection participan en la transaccion del Service.
 */
public class PrestamoRepository {

    public void probarConexion() {

        try (Connection conn = ConexionBD.obtenerConexion()) {

            System.out.println("Conexion exitosa a SQL Server");

        } catch (Exception e) {

            e.printStackTrace();

        }
    }

    public List<Equipo> listarEquiposDisponibles() {

        List<Equipo> lista = new ArrayList<>();

        String sql = "SELECT id, codigo_inventario, nombre "
                + "FROM Equipo WHERE estado = 'DISPONIBLE' "
                + "ORDER BY codigo_inventario";

        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Equipo equipo = new Equipo();
                equipo.setId(rs.getInt("id"));
                equipo.setCodigoInventario(rs.getString("codigo_inventario"));
                equipo.setNombre(rs.getString("nombre"));
                lista.add(equipo);
            }

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo cargar la lista de equipos disponibles.", e);
        }

        return lista;
    }

    // ------------------------------------------------------------------
    // Metodos transaccionales: reciben la Connection de Transaccion.
    // No capturan SQLException; si algo falla, Transaccion hace rollback.
    // ------------------------------------------------------------------

    public int insertarPrestamo(Connection con, Prestamo prestamo) throws SQLException {
        String sql = "INSERT INTO Prestamo (solicitante, fecha_prestamo, fecha_esperada_devolucion, "
                + "usuario_registra_id, estado, observaciones) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, prestamo.getSolicitante());
            ps.setTimestamp(2, new Timestamp(prestamo.getFechaPrestamo().getTime()));
            ps.setTimestamp(3, new Timestamp(prestamo.getFechaEsperadaDevolucion().getTime()));
            ps.setInt(4, prestamo.getUsuarioRegistraId());
            ps.setString(5, prestamo.getEstado().name());
            ps.setString(6, prestamo.getObservaciones());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("No se obtuvo el id del prestamo creado.");
    }

    public void insertarDetalle(Connection con, int prestamoId, int equipoId) throws SQLException {
        String sql = "INSERT INTO DetallePrestamo (prestamo_id, equipo_id) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, prestamoId);
            ps.setInt(2, equipoId);
            ps.executeUpdate();
        }
    }

    public String obtenerCodigoEquipo(Connection con, int equipoId) throws SQLException {
        String sql = "SELECT codigo_inventario FROM Equipo WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, equipoId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    public String obtenerEstadoEquipo(Connection con, int equipoId) throws SQLException {
        String sql = "SELECT estado FROM Equipo WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, equipoId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /** Cambia a PRESTADO solo si sigue DISPONIBLE. Devuelve false si otro proceso lo tomo antes. */
    public boolean marcarPrestadoSiDisponible(Connection con, int equipoId) throws SQLException {
        String sql = "UPDATE Equipo SET estado = 'PRESTADO' WHERE id = ? AND estado = 'DISPONIBLE'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, equipoId);
            return ps.executeUpdate() == 1;
        }
    }
}