package com.umg.sigelab.repository;

import com.umg.sigelab.exception.DataAccessException;
import com.umg.sigelab.model.NombreRol;
import com.umg.sigelab.model.Rol;
import com.umg.sigelab.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RolRepository {

    public List<Rol> listar() {
        String sql = "SELECT id, nombre FROM Rol ORDER BY id";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Rol> roles = new ArrayList<>();
            while (rs.next()) {
                roles.add(new Rol(rs.getInt("id"), NombreRol.valueOf(rs.getString("nombre"))));
            }
            return roles;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron consultar los roles.", e);
        }
    }
}
