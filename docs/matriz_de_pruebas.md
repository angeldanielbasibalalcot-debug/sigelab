# Matriz de casos de prueba - SIGELAB

Proyecto Final - Curso 017 Programacion II - UMG

Los resultados fueron registrados por el integrante Angel Daniel Basibal Alcot, ejecutando cada caso en la aplicacion y verificando el estado de los equipos en la pantalla de Equipos.

## Datos de partida
- Base de datos creada con `database/sigelab_schema.sql`, que inserta 13 equipos (10 DISPONIBLES, 1 DANADO, 1 en MANTENIMIENTO, 1 de BAJA). Durante las pruebas se crearon CAM-099 y TEST-001, que quedaron en BAJA; por eso el estado final de la base es de 15 equipos (10 DISPONIBLES, 1 DANADO, 1 en MANTENIMIENTO, 3 de BAJA).
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

## Notas sobre otros casos
- CP-13: la segunda devolucion se rechazo en el Service. Se probo abriendo dos instancias de la aplicacion: se devolvio el equipo en la primera y, desde la segunda con la lista desactualizada, el sistema lo rechazo. La proteccion adicional del UPDATE (condicion fecha_devolucion IS NULL) no se probo en vivo.
- CP-18: la fecha esperada de un prestamo se guarda a las 23:59:59 del dia elegido y la aplicacion no permite crear prestamos con fecha anterior a hoy. Por eso el prestamo vencido se simulo adelantando las fechas desde SSMS. El calculo de "vencido" (ACTIVO, con equipos pendientes y fecha esperada anterior a hoy) lo hace ReporteService.

## Limitaciones conocidas
- Horas: la fecha del prestamo la envia Java con la hora de la computadora (Guatemala), mientras que la fecha y hora de Auditoria y la fecha de salida de Mantenimiento las asigna SQL Server con SYSDATETIME(), y el contenedor de Docker esta en UTC. Por eso esos campos muestran 6 horas de diferencia. No afecta las reglas de negocio.
- Auditoria y usuarios sin rostro: un usuario sin rostro registrado es bloqueado con un mensaje claro, pero ese intento no deja registro LOGIN_FALLIDO, porque el bloqueo ocurre antes de que se registre el rastro. Los rostros rechazados si dejan LOGIN_FALLIDO.