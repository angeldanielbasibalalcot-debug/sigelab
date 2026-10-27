package com.umg.sigelab.repository;

import com.umg.sigelab.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;

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
}