package com.umg.sigelab.exception;

/**
 * Error tecnico de acceso a datos (SQL, conexion). Envuelve la excepcion original
 * para no mostrar detalles tecnicos al usuario.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
