package com.umg.sigelab;

import com.umg.sigelab.exception.DataAccessException;
import com.umg.sigelab.util.ConexionBD;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Punto de entrada temporal: solo prueba la conexion.
 * Se reemplazara por el LoginFrame cuando exista.
 */
public class Main {

    public static void main(String[] args) {
        try (Connection con = ConexionBD.obtenerConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM Equipo")) {
            rs.next();
            System.out.println("Conexion OK. Equipos en la base de datos: " + rs.getInt(1));
        } catch (DataAccessException | SQLException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
