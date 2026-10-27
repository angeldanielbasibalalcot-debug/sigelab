package com.umg.sigelab.repository;

import com.umg.sigelab.exception.DataAccessException;
import com.umg.sigelab.model.Equipo;
import com.umg.sigelab.model.EstadoEquipo;
import com.umg.sigelab.model.EstadoLaboratorio;
import com.umg.sigelab.model.EstadoMantenimiento;
import com.umg.sigelab.model.Laboratorio;
import com.umg.sigelab.model.Mantenimiento;
import com.umg.sigelab.model.ResultadoMantenimiento;
import com.umg.sigelab.model.TipoEquipo;
import com.umg.sigelab.model.TipoMantenimiento;
import com.umg.sigelab.util.ConexionBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MantenimientoRepository {

    private static final String CONSULTA_BASE = """
            SELECT
                m.id,
                m.fecha_ingreso,
                m.tipo,
                m.descripcion,
                m.responsable,
                m.fecha_salida,
                m.resultado,
                m.estado AS mant_estado,
                m.observaciones AS mant_observaciones,
                m.usuario_id,
                e.id AS equipo_id,
                e.codigo_inventario,
                e.nombre AS equipo_nombre,
                e.marca,
                e.modelo,
                e.numero_serie,
                e.estado AS equipo_estado,
                e.observaciones AS equipo_observaciones,
                t.id AS tipo_id,
                t.nombre AS tipo_nombre,
                t.descripcion AS tipo_descripcion,
                l.id AS laboratorio_id,
                l.codigo AS laboratorio_codigo,
                l.nombre AS laboratorio_nombre,
                l.ubicacion AS laboratorio_ubicacion,
                l.capacidad AS laboratorio_capacidad,
                l.estado AS laboratorio_estado
            FROM Mantenimiento m
            INNER JOIN Equipo e ON e.id = m.equipo_id
            INNER JOIN TipoEquipo t ON t.id = e.tipo_equipo_id
            INNER JOIN Laboratorio l ON l.id = e.laboratorio_id
            """;

    // Historial completo, el mas reciente primero.
    public List<Mantenimiento> listarTodos() {
        String sql = CONSULTA_BASE + " ORDER BY m.fecha_ingreso DESC, m.id DESC";
        return consultarLista(sql, null,
                "No se pudo consultar el historial de mantenimiento.");
    }

    // Solo los mantenimientos que siguen abiertos.
    public List<Mantenimiento> listarEnProceso() {
        String sql = CONSULTA_BASE
                + " WHERE m.estado = 'EN_PROCESO'"
                + " ORDER BY m.fecha_ingreso DESC, m.id DESC";
        return consultarLista(sql, null,
                "No se pudieron consultar los mantenimientos en proceso.");
    }

    // Historial de mantenimiento de un equipo (sirve tambien para reportes).
    public List<Mantenimiento> listarPorEquipo(int equipoId) {
        String sql = CONSULTA_BASE
                + " WHERE m.equipo_id = ?"
                + " ORDER BY m.fecha_ingreso DESC, m.id DESC";
        return consultarLista(sql, equipoId,
                "No se pudo consultar el historial de mantenimiento del equipo.");
    }

    public Optional<Mantenimiento> buscarPorId(int id) {
        String sql = CONSULTA_BASE + " WHERE m.id = ?";

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
                    "No se pudo consultar el mantenimiento.", e);
        }
    }

    // Para la regla: no puede haber dos mantenimientos EN_PROCESO del mismo equipo.
    public boolean existeEnProceso(int equipoId) {
        String sql = """
                SELECT COUNT(*)
                FROM Mantenimiento
                WHERE equipo_id = ? AND estado = 'EN_PROCESO'
                """;

        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, equipoId);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            throw new DataAccessException(
                    "No se pudo verificar si el equipo ya esta en mantenimiento.", e);
        }
    }

    // Recibe la conexion para compartir transaccion con el cambio de estado
    // del equipo y con la auditoria. La fecha de ingreso usa el DEFAULT de la tabla.
    public int insertar(Connection con, Mantenimiento m) throws SQLException {
        String sql = """
                INSERT INTO Mantenimiento (
                    equipo_id,
                    tipo,
                    descripcion,
                    responsable,
                    estado,
                    observaciones,
                    usuario_id
                )
                VALUES (?, ?, ?, ?, 'EN_PROCESO', ?, ?)
                """;

        try (PreparedStatement ps = con.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, m.getEquipo().getId());
            ps.setString(2, m.getTipo().name());
            ps.setString(3, m.getDescripcion());
            ps.setString(4, m.getResponsable());
            ps.setString(5, m.getObservaciones());
            ps.setInt(6, m.getUsuarioId());

            if (ps.executeUpdate() != 1) {
                throw new SQLException(
                        "No se inserto exactamente un mantenimiento.");
            }

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException(
                            "No se obtuvo el ID del mantenimiento creado.");
                }

                return keys.getInt(1);
            }
        }
    }

    // Cierra el mantenimiento: resultado, fecha de salida y estado FINALIZADO.
    // El WHERE incluye estado = 'EN_PROCESO' para que no se cierre dos veces.
    public void finalizar(Connection con, int id,
                          ResultadoMantenimiento resultado,
                          String observaciones) throws SQLException {
        String sql = """
                UPDATE Mantenimiento
                SET resultado = ?,
                    fecha_salida = SYSDATETIME(),
                    estado = 'FINALIZADO',
                    observaciones = COALESCE(?, observaciones)
                WHERE id = ? AND estado = 'EN_PROCESO'
                """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, resultado.name());
            ps.setString(2, observaciones);
            ps.setInt(3, id);

            if (ps.executeUpdate() != 1) {
                throw new SQLException(
                        "No se finalizo exactamente un mantenimiento.");
            }
        }
    }

    // Cambia SOLO el estado del equipo. Vive aqui para no modificar
    // EquipoRepository, que pertenece a otro modulo.
    public void actualizarEstadoEquipo(Connection con, int equipoId,
                                       EstadoEquipo nuevoEstado)
            throws SQLException {
        String sql = "UPDATE Equipo SET estado = ? WHERE id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nuevoEstado.name());
            ps.setInt(2, equipoId);

            if (ps.executeUpdate() != 1) {
                throw new SQLException(
                        "No se actualizo exactamente un equipo.");
            }
        }
    }

    private List<Mantenimiento> consultarLista(String sql, Integer parametro,
                                               String mensajeError) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (parametro != null) {
                ps.setInt(1, parametro);
            }

            try (ResultSet rs = ps.executeQuery()) {
                List<Mantenimiento> lista = new ArrayList<>();

                while (rs.next()) {
                    lista.add(mapear(rs));
                }

                return lista;
            }

        } catch (SQLException e) {
            throw new DataAccessException(mensajeError, e);
        }
    }

    private Mantenimiento mapear(ResultSet rs) throws SQLException {
        TipoEquipo tipoEquipo = new TipoEquipo(
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
                EstadoLaboratorio.valueOf(rs.getString("laboratorio_estado"))
        );

        Equipo equipo = new Equipo(
                rs.getInt("equipo_id"),
                rs.getString("codigo_inventario"),
                rs.getString("equipo_nombre"),
                tipoEquipo,
                rs.getString("marca"),
                rs.getString("modelo"),
                rs.getString("numero_serie"),
                laboratorio,
                EstadoEquipo.valueOf(rs.getString("equipo_estado")),
                rs.getString("equipo_observaciones")
        );

        Mantenimiento m = new Mantenimiento();
        m.setId(rs.getInt("id"));
        m.setEquipo(equipo);
        m.setFechaIngreso(rs.getTimestamp("fecha_ingreso").toLocalDateTime());
        m.setTipo(TipoMantenimiento.valueOf(rs.getString("tipo")));
        m.setDescripcion(rs.getString("descripcion"));
        m.setResponsable(rs.getString("responsable"));

        Timestamp salida = rs.getTimestamp("fecha_salida");
        if (salida != null) {
            m.setFechaSalida(salida.toLocalDateTime());
        }

        String resultado = rs.getString("resultado");
        if (resultado != null) {
            m.setResultado(ResultadoMantenimiento.valueOf(resultado));
        }

        m.setEstado(EstadoMantenimiento.valueOf(rs.getString("mant_estado")));
        m.setObservaciones(rs.getString("mant_observaciones"));
        m.setUsuarioId(rs.getInt("usuario_id"));

        return m;
    }
}