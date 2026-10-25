

package com.umg.sigelab.repository;

import com.umg.sigelab.util.ConexionBD;

import java.sql.Connection;

public class PrestamoRepository {

    public void probarConexion() {

        try (Connection conn = ConexionBD.obtenerConexion()) {

            System.out.println("Conexion exitosa a SQL Server");

        } catch (Exception e) {

            e.printStackTrace();

        }
    }
}
