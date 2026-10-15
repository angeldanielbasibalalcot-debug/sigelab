package com.umg.sigelab.exception;

/**
 * Error de regla de negocio (RN-xx). El mensaje se puede mostrar
 * directamente al usuario. Ejemplo: "Ya existe un equipo con el codigo CAM-001".
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
