package com.umg.sigelab.util;

import com.umg.sigelab.exception.DataAccessException;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Ejecuta varias operaciones SQL como UNA sola transaccion:
 * si todo sale bien se hace commit; si algo falla se hace rollback y no queda nada a medias.
 *
 * Uso tipico en un Service (ej. confirmar un prestamo):
 *
 *     Transaccion.ejecutarSinResultado(con -> {
 *         int id = prestamoRepository.insertar(con, prestamo);
 *         detalleRepository.insertar(con, id, equipos);
 *         equipoRepository.cambiarEstado(con, equipoId, EstadoEquipo.PRESTADO);
 *         auditoriaRepository.registrar(con, usuarioId, AccionAuditoria.PRESTAMO_CREADO, "Prestamo", id, "...");
 *     });
 *
 * Los metodos de los Repository que participan reciben la Connection como parametro.
 */
public final class Transaccion {

    @FunctionalInterface
    public interface Operacion<T> {
        T ejecutar(Connection con) throws SQLException;
    }

    @FunctionalInterface
    public interface OperacionSinResultado {
        void ejecutar(Connection con) throws SQLException;
    }

    private Transaccion() {
    }

    public static <T> T ejecutar(Operacion<T> operacion) {
        try (Connection con = ConexionBD.obtenerConexion()) {
            con.setAutoCommit(false);
            try {
                T resultado = operacion.ejecutar(con);
                con.commit();
                return resultado;
            } catch (SQLException e) {
                revertir(con);
                throw new DataAccessException("No se pudo completar la operación. No se realizaron cambios.", e);
            } catch (RuntimeException e) {
                revertir(con);
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al manejar la transacción.", e);
        }
    }

    public static void ejecutarSinResultado(OperacionSinResultado operacion) {
        ejecutar(con -> {
            operacion.ejecutar(con);
            return null;
        });
    }

    private static void revertir(Connection con) {
        try {
            con.rollback();
        } catch (SQLException ignorada) {
            // Si el rollback falla, la conexion se cierra y el servidor descarta la transaccion.
        }
    }
}
