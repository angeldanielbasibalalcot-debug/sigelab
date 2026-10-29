# Matriz de casos de prueba - SIGELAB

Proyecto Final - Curso 017 Programacion II - UMG

Los resultados fueron registrados por el integrante Angel Daniel Basibal Alcot, ejecutando cada caso en la aplicacion y verificando el estado de los equipos en la pantalla de Equipos.

## Datos de partida
- Base de datos creada con `database/sigelab_schema.sql`: 14 equipos (10 DISPONIBLES, 1 DANADO, 1 en MANTENIMIENTO, 2 de BAJA).
- Usuarios de prueba: admin, encargado, docente e inactivo (ver README.md).
- Componente facial iniciado antes de la aplicacion (ver README.md).

## Matriz

| Caso | Modulo | Prueba | Resultado esperado | Reglas | Estado |
|---|---|---|---|---|---|
| CP-01 | Login | Usuario valido + contrasena valida + rostro correcto | Acceso permitido | RN-02 | Probado |
| CP-02 | Login | Usuario valido + contrasena valida + rostro incorrecto | Acceso denegado | RN-02 | Probado |
| CP-03 | Equipo | Registrar segundo equipo con el mismo codigo | Registro rechazado | RN-04 | Probado |
| CP-04 | Prestamo | Prestar equipo DISPONIBLE | Prestamo creado; equipo pasa a PRESTADO | RN-07, RN-09 | Probado |
| CP-05 | Prestamo | Intentar prestar nuevamente el mismo equipo | Operacion rechazada | RN-08 | Probado |
| CP-06 | Prestamo | Intentar prestar equipo DANADO | Operacion rechazada | RN-07, RN-08 | Probado |
| CP-07 | Prestamo | Intentar prestar equipo en MANTENIMIENTO | Operacion rechazada | RN-08, RN-14 | Probado |
| CP-08 | Prestamo | Intentar prestar equipo BAJA | Operacion rechazada | RN-08 | Probado |
| CP-09 | QR | Escanear QR de equipo DISPONIBLE | Equipo identificado y disponible para agregar | RN-07, RN-19 | Probado |
| CP-10 | QR | Escanear QR de equipo PRESTADO | Equipo identificado, pero bloqueado | RN-08, RN-19 | Probado |
| CP-11 | Devolucion | Devolver equipo en condicion BUENA | Equipo pasa a DISPONIBLE | RN-10, RN-12 | Probado |
| CP-12 | Devolucion | Devolver equipo como DANADO | Equipo pasa a DANADO | RN-13 | Probado |
| CP-13 | Devolucion | Intentar devolver nuevamente un equipo ya devuelto | Operacion rechazada | RN-11 | Probado |
| CP-14 | Devolucion | Devolver 2 de 3 equipos | Prestamo continua activo con 1 pendiente | RN-16 | Probado |
| CP-15 | Mantenimiento | Enviar equipo DANADO a mantenimiento | Equipo pasa a MANTENIMIENTO | RN-18 | Probado |
| CP-16 | Mantenimiento | Cerrar mantenimiento como reparado | Equipo pasa a DISPONIBLE | RN-15 | Probado |
| CP-17 | Mantenimiento | Cerrar mantenimiento como no reparable | Equipo pasa a BAJA | RN-15 | Probado |
| CP-18 | Reportes | Fecha esperada vencida con equipo pendiente | El prestamo aparece como vencido | RN-17 | Probado |

## Notas de los casos de login
- CP-01: entran admin y encargado con su rostro registrado.
- CP-02: un rostro ajeno y una cara tapada son rechazados, tanto directamente en la API facial como en el login real.
- Un usuario sin rostro registrado es denegado con un mensaje claro, y si la API facial esta apagada el acceso se deniega sin mostrar errores tecnicos.
- Los eventos relevantes (login, prestamos, devoluciones, mantenimiento, rostros) quedan registrados en Auditoria (RN-18).
