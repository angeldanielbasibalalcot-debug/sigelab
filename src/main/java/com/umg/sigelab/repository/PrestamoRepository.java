package com.umg.sigelab.repository;

import com.umg.sigelab.model.Equipo;
import com.umg.sigelab.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.umg.sigelab.model.Prestamo;

import java.sql.Statement;

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

        String sql =
                "SELECT * " +
                        "FROM Equipo " +
                        "WHERE estado='DISPONIBLE'";

        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                Equipo equipo = new Equipo();

                equipo.setId(rs.getInt("id"));
                equipo.setCodigoInventario(
                        rs.getString("codigo_inventario"));

                equipo.setNombre(
                        rs.getString("nombre"));

                lista.add(equipo);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return lista;
    }
    public int guardarPrestamo(Prestamo prestamo) {

        String sql =
                "INSERT INTO Prestamo (" +
                        "solicitante," +
                        "fecha_prestamo," +
                        "fecha_esperada_devolucion," +
                        "usuario_registra_id," +
                        "estado," +
                        "observaciones" +
                        ") VALUES (?,?,?,?,?,?)";

        try (
                Connection con = ConexionBD.obtenerConexion();

                PreparedStatement ps =
                        con.prepareStatement(
                                sql,
                                Statement.RETURN_GENERATED_KEYS)
        ) {

            ps.setString(
                    1,
                    prestamo.getSolicitante());

            ps.setDate(
                    2,
                    new java.sql.Date(
                            prestamo.getFechaPrestamo().getTime()));

            ps.setDate(
                    3,
                    new java.sql.Date(
                            prestamo.getFechaEsperadaDevolucion().getTime()));

            ps.setInt(
                    4,
                    prestamo.getUsuarioRegistraId());

            ps.setString(
                    5,
                    prestamo.getEstado().name());

            ps.setString(
                    6,
                    prestamo.getObservaciones());

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (Exception e) {

            e.printStackTrace();

        }

        return 0;
    }

}