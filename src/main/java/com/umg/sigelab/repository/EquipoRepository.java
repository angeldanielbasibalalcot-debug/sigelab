package com.umg.sigelab.repository;

import com.umg.sigelab.exception.DataAccessException;
import com.umg.sigelab.model.Equipo;
import com.umg.sigelab.model.EstadoEquipo;
import com.umg.sigelab.model.EstadoLaboratorio;
import com.umg.sigelab.model.Laboratorio;
import com.umg.sigelab.model.TipoEquipo;
import com.umg.sigelab.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EquipoRepository {

    // JOIN para traer el equipo junto con su tipo y su laboratorio en una sola consulta.
    private static final String SELECT_BASE =
            "SELECT e.id, e.codigo_inventario, e.nombre, e.marca, e.modelo, e.numero_serie, "
                    + "e.estado, e.observaciones, "
                    + "t.id AS tipo_id, t.nombre AS tipo_nombre, t.descripcion AS tipo_descripcion, "
                    + "l.id AS lab_id, l.codigo AS lab_codigo, l.nombre AS lab_nombre, "
                    + "l.ubicacion AS lab_ubicacion, l.capacidad AS lab_capacidad, l.estado AS lab_estado "
                    + "FROM Equipo e "
                    + "INNER JOIN TipoEquipo t ON t.id = e.tipo_equipo_id "
                    + "INNER JOIN Laboratorio l ON l.id = e.laboratorio_id ";

    public List<Equipo> listarTodos() {
        String sql = SELECT_BASE + "ORDER BY e.codigo_inventario";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Equipo> equipos = new ArrayList<>();
            while (rs.next()) {
                equipos.add(mapear(rs));
            }
            return equipos;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron consultar los equipos.", e);
        }
    }

    public Optional<Equipo> buscarPorId(int id) {
        String sql = SELECT_BASE + "WHERE e.id = ?";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar el equipo.", e);
        }
    }

    /** Busca por codigo de inventario. Es lo que usa la lectura del QR (6.6): el QR trae el codigo. */
    public Optional<Equipo> buscarPorCodigo(String codigoInventario) {
        String sql = SELECT_BASE + "WHERE e.codigo_inventario = ?";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigoInventario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar el equipo por su codigo.", e);
        }
    }

    /** True si ya hay OTRO equipo con ese codigo de inventario (RN-04). Al crear se pasa idExcluir = 0. */
    public boolean existeCodigo(String codigoInventario, int idExcluir) {
        String sql = "SELECT COUNT(*) FROM Equipo WHERE codigo_inventario = ? AND id <> ?";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigoInventario);
            ps.setInt(2, idExcluir);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo verificar el codigo de inventario.", e);
        }
    }

    /** Inserta y devuelve el id generado. Recibe la Connection para usarse dentro de una Transaccion. */
    public int insertar(Connection con, Equipo equipo) throws SQLException {
        String sql = "INSERT INTO Equipo (codigo_inventario, nombre, tipo_equipo_id, marca, modelo, "
                + "numero_serie, laboratorio_id, estado, observaciones) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, equipo.getCodigoInventario());
            ps.setString(2, equipo.getNombre());
            ps.setInt(3, equipo.getTipo().getId());
            ps.setString(4, equipo.getMarca());
            ps.setString(5, equipo.getModelo());
            ps.setString(6, equipo.getNumeroSerie());
            ps.setInt(7, equipo.getLaboratorio().getId());
            ps.setString(8, equipo.getEstado().name());
            ps.setString(9, equipo.getObservaciones());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    /** Actualiza los datos del equipo. No toca el codigo ni el estado: el estado lo cambian las operaciones. */
    public void actualizar(Connection con, Equipo equipo) throws SQLException {
        String sql = "UPDATE Equipo SET nombre = ?, tipo_equipo_id = ?, marca = ?, modelo = ?, "
                + "numero_serie = ?, laboratorio_id = ?, observaciones = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, equipo.getNombre());
            ps.setInt(2, equipo.getTipo().getId());
            ps.setString(3, equipo.getMarca());
            ps.setString(4, equipo.getModelo());
            ps.setString(5, equipo.getNumeroSerie());
            ps.setInt(6, equipo.getLaboratorio().getId());
            ps.setString(7, equipo.getObservaciones());
            ps.setInt(8, equipo.getId());
            ps.executeUpdate();
        }
    }

    private Equipo mapear(ResultSet rs) throws SQLException {
        TipoEquipo tipo = new TipoEquipo(
                rs.getInt("tipo_id"),
                rs.getString("tipo_nombre"),
                rs.getString("tipo_descripcion"));
        Laboratorio laboratorio = new Laboratorio(
                rs.getInt("lab_id"),
                rs.getString("lab_codigo"),
                rs.getString("lab_nombre"),
                rs.getString("lab_ubicacion"),
                rs.getInt("lab_capacidad"),
                EstadoLaboratorio.valueOf(rs.getString("lab_estado")));
        return new Equipo(
                rs.getInt("id"),
                rs.getString("codigo_inventario"),
                rs.getString("nombre"),
                tipo,
                rs.getString("marca"),
                rs.getString("modelo"),
                rs.getString("numero_serie"),
                laboratorio,
                EstadoEquipo.valueOf(rs.getString("estado")),
                rs.getString("observaciones"));
    }
}