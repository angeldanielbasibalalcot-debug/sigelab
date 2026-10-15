package com.umg.sigelab.util;

import com.umg.sigelab.exception.DataAccessException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Entrega conexiones JDBC a SQL Server. Lee los datos de "config.properties"
 * (en la raiz del proyecto). Solo los Repository deben usar esta clase.
 */
public final class ConexionBD {

    private static final Path ARCHIVO_CONFIG = Path.of("config.properties");
    private static Properties config;

    private ConexionBD() {
    }

    public static Connection obtenerConexion() {
        Properties p = obtenerConfig();
        try {
            return DriverManager.getConnection(
                    p.getProperty("db.url"),
                    p.getProperty("db.user"),
                    p.getProperty("db.password"));
        } catch (SQLException e) {
            throw new DataAccessException(
                    "No se pudo conectar a la base de datos. Verifique que SQL Server este en ejecucion "
                            + "y que los datos de config.properties sean correctos.", e);
        }
    }

    private static synchronized Properties obtenerConfig() {
        if (config == null) {
            if (!Files.exists(ARCHIVO_CONFIG)) {
                throw new DataAccessException(
                        "No se encontro config.properties. Copie config.properties.example "
                                + "como config.properties y configure su conexion.");
            }
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(ARCHIVO_CONFIG)) {
                p.load(in);
            } catch (IOException e) {
                throw new DataAccessException("No se pudo leer config.properties.", e);
            }
            config = p;
        }
        return config;
    }
}
