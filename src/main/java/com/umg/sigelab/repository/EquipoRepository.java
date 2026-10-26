package com.umg.sigelab.repository;

import com.umg.sigelab.exception.DataAccessException;
import com.umg.sigelab.model.Equipo;
import com.umg.sigelab.model.EstadoEquipo;
import com.umg.sigelab.model.EstadoLaboratorio;
import com.umg.sigelab.model.Laboratorio;
import com.umg.sigelab.model.TipoEquipo;
import com.umg.sigelab.util.ConexionBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EquipoRepository {

    private static final String CONSULTA_BASE = """
            SELECT
                e.id,
                e.codigo_inventario,
                e.nombre,
                e.marca,
                e.modelo,
                e.numero_serie,
                e.estado,
                e.observaciones,
                t.id AS tipo_id,
                t.nombre AS tipo_nombre,
                t.descripcion AS tipo_descripcion,
                l.id AS laboratorio_id,
                l.codigo AS laboratorio_codigo,
                l.nombre AS laboratorio_nombre,
                l.ubicacion AS laboratorio_ubicacion,
                l.capacidad AS laboratorio_capacidad,
                l.estado AS laboratorio_estado
            FROM Equipo e
            INNER JOIN TipoEquipo t ON t.id = e.tipo_equipo_id
            INNER JOIN Laboratorio l ON l.id = e.laboratorio_id
            """;

    public List<Equipo> listarTodos() {
        String sql = CONSULTA_BASE + " ORDER BY e.codigo_inventario";

        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Equipo> equipos = new ArrayList<>();

            while (rs.next()) {
                equipos.add(mapear(rs));
            }

            return equipos;

        } catch (SQLException e) {
            throw new DataAccessException(
                    "No se pudieron consultar los equipos.", e);
        }
    }

    public Optional<Equipo> buscarPorId(int id) {
        String sql = CONSULTA_BASE + " WHERE e.id = ?";

        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? Optional.of(mapear(rs))
                        : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DataAccessException(
                    "No se pudo consultar el equipo.", e);
        }
    }

    // También servirá para buscar un equipo identificado mediante QR.
    public Optional<Equipo> buscarPorCodigo(String codigo) {
        String sql = CONSULTA_BASE + " WHERE e.codigo_inventario = ?";

        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, codigo);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? Optional.of(mapear(rs))
                        : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DataAccessException(
                    "No se pudo buscar el equipo por código.", e);
        }
    }

    // Al crear se pasa 0; al editar se excluye el ID del propio equipo.
    public boolean existeCodigo(String codigo, int idExcluir) {
        String sql = """
                SELECT COUNT(*)
                FROM Equipo
                WHERE codigo_inventario = ? AND id <> ?
                """;

        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, codigo);
            ps.setInt(2, idExcluir);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            throw new DataAccessException(
                    "No se pudo verificar el código de inventario.", e);
        }
    }

    // Recibe la conexión para compartir transacción con la auditoría.
    public int insertar(Connection con, Equipo equipo)
            throws SQLException {

        String sql = """
                INSERT INTO Equipo (
                    codigo_inventario,
                    nombre,
                    tipo_equipo_id,
                    marca,
                    modelo,
                    numero_serie,
                    laboratorio_id,
                    estado,
                    observaciones
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = con.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, equipo.getCodigoInventario());
            ps.setString(2, equipo.getNombre());
            ps.setInt(3, equipo.getTipoEquipo().getId());
            ps.setString(4, equipo.getMarca());
            ps.setString(5, equipo.getModelo());
            ps.setString(6, equipo.getNumeroSerie());
            ps.setInt(7, equipo.getLaboratorio().getId());
            ps.setString(8, equipo.getEstado().name());
            ps.setString(9, equipo.getObservaciones());

            if (ps.executeUpdate() != 1) {
                throw new SQLException(
                        "No se insertó exactamente un equipo.");
            }

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException(
                            "No se obtuvo el ID del equipo creado.");
                }

                return keys.getInt(1);
            }
        }
    }

    // Edita datos del catálogo. El estado lo gestionan los procesos
    // de préstamo, devolución y mantenimiento.
    public void actualizar(Connection con, Equipo equipo)
            throws SQLException {

        String sql = """
                UPDATE Equipo
                SET codigo_inventario = ?,
                    nombre = ?,
                    tipo_equipo_id = ?,
                    marca = ?,
                    modelo = ?,
                    numero_serie = ?,
                    laboratorio_id = ?,
                    observaciones = ?
                WHERE id = ?
                """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, equipo.getCodigoInventario());
            ps.setString(2, equipo.getNombre());
            ps.setInt(3, equipo.getTipoEquipo().getId());
            ps.setString(4, equipo.getMarca());
            ps.setString(5, equipo.getModelo());
            ps.setString(6, equipo.getNumeroSerie());
            ps.setInt(7, equipo.getLaboratorio().getId());
            ps.setString(8, equipo.getObservaciones());
            ps.setInt(9, equipo.getId());

            if (ps.executeUpdate() != 1) {
                throw new SQLException(
                        "No se actualizó exactamente un equipo.");
            }
        }
    }

    private Equipo mapear(ResultSet rs) throws SQLException {
        TipoEquipo tipo = new TipoEquipo(
                rs.getInt("tipo_id"),
                rs.getString("tipo_nombre"),
                rs.getString("tipo_descripcion")
        );

        Laboratorio laboratorio = new Laboratorio(
                rs.getInt("laboratorio_id"),
                rs.getString("laboratorio_codigo"),
                rs.getString("laboratorio_nombre"),
                rs.getString("laboratorio_ubicacion"),
                rs.getInt("laboratorio_capacidad"),
                EstadoLaboratorio.valueOf(
                        rs.getString("laboratorio_estado"))
        );

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
                rs.getString("observaciones")
        );
    }
}
