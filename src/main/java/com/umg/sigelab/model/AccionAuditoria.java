package com.umg.sigelab.model;

/** Acciones que se registran en la tabla Auditoria (seccion 6.10 de la guia + gestion de usuarios). */
public enum AccionAuditoria {
    LOGIN_EXITOSO,
    LOGIN_FALLIDO,
    PRESTAMO_CREADO,
    EQUIPO_DEVUELTO,
    EQUIPO_ENVIADO_MANTENIMIENTO,
    MANTENIMIENTO_FINALIZADO,
    USUARIO_DESACTIVADO,
    USUARIO_CREADO,
    USUARIO_ACTUALIZADO,
    USUARIO_REACTIVADO
}
