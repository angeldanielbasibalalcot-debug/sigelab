package com.umg.sigelab.repository;

import com.umg.sigelab.exception.DataAccessException;
import com.umg.sigelab.model.EstadoLaboratorio;
import com.umg.sigelab.model.Laboratorio;
import com.umg.sigelab.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LaboratorioRepository {

    private static final String COLUMNAS = "id, codigo, nombre, ubicacion, capacidad, estado";

    public List<Laboratorio> listarTodos() {
        String sql = "SELECT " + COLUMNAS + " FROM Laboratorio ORDER BY codigo";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Laboratorio> laboratorios = new ArrayList<>();
            while (rs.next()) {
                laboratorios.add(mapear(rs));
            }
            return laboratorios;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron consultar los laboratorios.", e);
        }
    }

    public Optional<Laboratorio> buscarPorId(int id) {
        String sql = "SELECT " + COLUMNAS + " FROM Laboratorio WHERE id = ?";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar el laboratorio.", e);
        }
    }

    /** True si ya hay OTRO laboratorio con ese código. Al crear se pasa idExcluir = 0. */
    public boolean existeCodigo(String codigo, int idExcluir) {
        String sql = "SELECT COUNT(*) FROM Laboratorio WHERE codigo = ? AND id <> ?";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigo);
            ps.setInt(2, idExcluir);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo verificar el código del laboratorio.", e);
        }
    }

    /** Inserta y devuelve el id generado. Recibe la Connection para usarse dentro de una Transaccion. */
    public int insertar(Connection con, Laboratorio lab) throws SQLException {
        String sql = "INSERT INTO Laboratorio (codigo, nombre, ubicacion, capacidad, estado) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, lab.getCodigo());
            ps.setString(2, lab.getNombre());
            ps.setString(3, lab.getUbicacion());
            ps.setInt(4, lab.getCapacidad());
            ps.setString(5, lab.getEstado().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public void actualizar(Connection con, Laboratorio lab) throws SQLException {
        String sql = "UPDATE Laboratorio SET codigo = ?, nombre = ?, ubicacion = ?, capacidad = ? "
                + "WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, lab.getCodigo());
            ps.setString(2, lab.getNombre());
            ps.setString(3, lab.getUbicacion());
            ps.setInt(4, lab.getCapacidad());
            ps.setInt(5, lab.getId());
            ps.executeUpdate();
        }
    }

    public void cambiarEstado(Connection con, int id, EstadoLaboratorio estado) throws SQLException {
        String sql = "UPDATE Laboratorio SET estado = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado.name());
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    private Laboratorio mapear(ResultSet rs) throws SQLException {
        return new Laboratorio(
                rs.getInt("id"),
                rs.getString("codigo"),
                rs.getString("nombre"),
                rs.getString("ubicacion"),
                rs.getInt("capacidad"),
                EstadoLaboratorio.valueOf(rs.getString("estado")));
    }
}
