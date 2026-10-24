package com.umg.sigelab.repository;

import com.umg.sigelab.exception.DataAccessException;
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

public class TipoEquipoRepository {

    public List<TipoEquipo> listarTodos() {
        String sql = "SELECT id, nombre, descripcion FROM TipoEquipo ORDER BY nombre";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<TipoEquipo> tipos = new ArrayList<>();
            while (rs.next()) {
                tipos.add(mapear(rs));
            }
            return tipos;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron consultar los tipos de equipo.", e);
        }
    }

    public Optional<TipoEquipo> buscarPorId(int id) {
        String sql = "SELECT id, nombre, descripcion FROM TipoEquipo WHERE id = ?";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar el tipo de equipo.", e);
        }
    }

    /** True si ya hay OTRO tipo con ese nombre. Al crear se pasa idExcluir = 0. */
    public boolean existeNombre(String nombre, int idExcluir) {
        String sql = "SELECT COUNT(*) FROM TipoEquipo WHERE nombre = ? AND id <> ?";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setInt(2, idExcluir);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo verificar el nombre del tipo de equipo.", e);
        }
    }

    /** Inserta y devuelve el id generado. Recibe la Connection para usarse dentro de una Transaccion. */
    public int insertar(Connection con, TipoEquipo tipo) throws SQLException {
        String sql = "INSERT INTO TipoEquipo (nombre, descripcion) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, tipo.getNombre());
            ps.setString(2, tipo.getDescripcion());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public void actualizar(Connection con, TipoEquipo tipo) throws SQLException {
        String sql = "UPDATE TipoEquipo SET nombre = ?, descripcion = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tipo.getNombre());
            ps.setString(2, tipo.getDescripcion());
            ps.setInt(3, tipo.getId());
            ps.executeUpdate();
        }
    }

    private TipoEquipo mapear(ResultSet rs) throws SQLException {
        return new TipoEquipo(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getString("descripcion"));
    }
}