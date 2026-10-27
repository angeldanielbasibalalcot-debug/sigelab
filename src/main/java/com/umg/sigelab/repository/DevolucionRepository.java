package com.umg.sigelab.repository;

import com.umg.sigelab.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import com.umg.sigelab.model.DetallePrestamoCombo;
import java.util.ArrayList;
import java.util.List;

import java.sql.ResultSet;

public class DevolucionRepository {

    public void devolverEquipo(
            int detallePrestamoId,
            String condicion,
            String observacion,
            int usuarioRecibeId) {

        String sql =
                "UPDATE DetallePrestamo " +
                        "SET fecha_devolucion = GETDATE()," +
                        "condicion_devolucion = ?," +
                        "observacion_devolucion = ?," +
                        "usuario_recibe_id = ? " +
                        "WHERE id = ?";

        try (
                Connection con =
                        ConexionBD.obtenerConexion();

                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setString(1, condicion);
            ps.setString(2, observacion);
            ps.setInt(3, usuarioRecibeId);
            ps.setInt(4, detallePrestamoId);

            ps.executeUpdate();

        } catch (Exception e) {

            e.printStackTrace();

        }
    }
    public void actualizarEstadoDisponible(
            int detallePrestamoId) {

        String sqlBuscar =
                "SELECT equipo_id " +
                        "FROM DetallePrestamo " +
                        "WHERE id = ?";

        String sqlActualizar =
                "UPDATE Equipo " +
                        "SET estado = 'DISPONIBLE' " +
                        "WHERE id = ?";

        try (
                Connection con =
                        ConexionBD.obtenerConexion()
        ) {

            int equipoId = 0;

            try (
                    PreparedStatement ps =
                            con.prepareStatement(
                                    sqlBuscar)
            ) {

                ps.setInt(
                        1,
                        detallePrestamoId);

                ResultSet rs =
                        ps.executeQuery();

                if (rs.next()) {

                    equipoId =
                            rs.getInt(
                                    "equipo_id");
                }
            }

            if (equipoId > 0) {

                try (
                        PreparedStatement ps =
                                con.prepareStatement(
                                        sqlActualizar)
                ) {

                    ps.setInt(
                            1,
                            equipoId);

                    ps.executeUpdate();
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

        }
    }
    public void finalizarPrestamo(
            int detallePrestamoId) {

        String sql =
                "UPDATE Prestamo " +
                        "SET estado = 'FINALIZADO' " +
                        "WHERE id = (" +
                        "SELECT prestamo_id " +
                        "FROM DetallePrestamo " +
                        "WHERE id = ?)";

        try (
                Connection con =
                        ConexionBD.obtenerConexion();

                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setInt(
                    1,
                    detallePrestamoId);

            ps.executeUpdate();

        } catch (Exception e) {

            e.printStackTrace();

        }
    }
    public List<DetallePrestamoCombo> listarPendientes() {

        List<DetallePrestamoCombo> lista =
                new ArrayList<>();

        String sql =
                "SELECT dp.id, " +
                        "p.id AS prestamo_id, " +
                        "e.codigo_inventario, " +
                        "e.nombre " +
                        "FROM DetallePrestamo dp " +
                        "INNER JOIN Prestamo p " +
                        "ON dp.prestamo_id = p.id " +
                        "INNER JOIN Equipo e " +
                        "ON dp.equipo_id = e.id " +
                        "WHERE dp.fecha_devolucion IS NULL";

        try (
                Connection con =
                        ConexionBD.obtenerConexion();

                PreparedStatement ps =
                        con.prepareStatement(sql);

                ResultSet rs =
                        ps.executeQuery()
        ) {

            while (rs.next()) {

                lista.add(
                        new DetallePrestamoCombo(
                                rs.getInt("id"),
                                rs.getString("codigo_inventario")
                                        + " - "
                                        + rs.getString("nombre")
                                        + " (Prestamo "
                                        + rs.getInt("prestamo_id")
                                        + ")"
                        )
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

        }

        return lista;
    }
}