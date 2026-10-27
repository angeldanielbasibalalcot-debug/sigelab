package com.umg.sigelab.repository;

import com.umg.sigelab.model.Equipo;
import com.umg.sigelab.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

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
}