# Manual tecnico - SIGELAB

Proyecto Final - Curso 017 Programacion II - UMG

## 1. Proposito
SIGELAB controla el ciclo de vida de los equipos de los laboratorios universitarios: disponible, prestado, danado, en mantenimiento y de baja. Este manual explica como esta construido el sistema y donde vive cada regla de negocio.

## 2. Arquitectura por capas
Flujo: View -> Controller -> Service -> Repository -> JDBC -> SQL Server.

| Capa | Paquete | Responsabilidad |
|---|---|---|
| View | com.umg.sigelab.view | Formularios Swing (.form del GUI Designer). Capturan y muestran datos; no contienen SQL. |
| Controller | com.umg.sigelab.controller | Coordina la interfaz con los servicios; solo delega. |
| Service | com.umg.sigelab.service | Reglas de negocio, permisos, transacciones y auditoria. |
| Repository | com.umg.sigelab.repository | Todo el SQL, con JDBC y consultas parametrizadas. |
| Model | com.umg.sigelab.model | Entidades y enumeraciones (estados, roles, acciones de auditoria). |
| Exception | com.umg.sigelab.exception | BusinessException y DataAccessException. |
| Util | com.umg.sigelab.util | ConexionBD, Transaccion, SesionActual, PasswordUtil y QrUtil. |

Contenido actual: 11 formularios mas MainFrame, 11 controladores, 13 servicios y 10 repositorios.

## 3. Roles y permisos (RN-20)
El menu de MainFrame se adapta al rol, pero ocultar una opcion no es la seguridad: cada Service vuelve a validar el rol con AutorizacionService al inicio de cada metodo publico, de modo que una regla no depende de la interfaz.

| Modulo | ADMIN | ENCARGADO | DOCENTE |
|---|---|---|---|
| Usuarios, Laboratorios, Tipos de equipo | Si | No | No |
| Equipos | Si | Si | No |
| Prestamos, Devoluciones, Mantenimiento | Si | Si | No |
| Reportes | Si | Si | Si |
| Auditoria | Si | No | No |

## 4. Inicio de sesion (RN-01, RN-02, RN-18)
AuthService valida en este orden: usuario y contrasena, estado ACTIVO (RN-01: un usuario INACTIVO no entra) y validacion facial (RN-02). El login exitoso y el fallido quedan en Auditoria (RN-18). Las contrasenas se guardan como hash, no en texto plano (PasswordUtil).

## 5. Reconocimiento facial
- Componente: API local en Python (Flask + OpenCV, algoritmo LBPH) en 127.0.0.1:5001, carpeta facial/.
- Java la consume con HttpClient desde FacialService. Metodos: verificar, tieneRostro, registrarRostro y eliminarRostro.
- Endpoints: GET /salud, GET /estado/<id>, POST /registrar/<id> (20 muestras), POST /verificar/<id> (5 capturas; se compara la mediana de distancias contra el umbral) y DELETE /rostro/<id>.
- verificar devuelve true solo si la API responde ok:true. Si la API no responde, el acceso se deniega con un mensaje comprensible, sin errores tecnicos. Un usuario sin rostro registrado no puede entrar.
- Registrar y eliminar rostros es solo para ADMIN (UsuarioService) y deja auditoria (ROSTRO_REGISTRADO y ROSTRO_ELIMINADO).
- Los rostros se guardan como archivos en facial/rostros/<id de usuario>/, no en SQL Server, y no se suben a Git.

## 6. Codigo QR (RN-19)
El QR contiene el codigo de inventario del equipo (por ejemplo CAM-001) y se genera y lee con ZXing (QrUtil y QrService). PrestamoService identifica el equipo a partir de la imagen del QR buscando el codigo en la base de datos. La lectura solo identifica: no cambia estados (RN-19). Al confirmar el prestamo, el Service vuelve a validar que el equipo este DISPONIBLE.

## 7. Transacciones y manejo de errores
- Las operaciones que escriben usan Transaccion.ejecutar o Transaccion.ejecutarSinResultado: la operacion y su registro de auditoria se confirman juntos o se deshacen juntos. Si falla una sentencia SQL en medio de un prestamo, no queda informacion a medias.
- Los Repository convierten SQLException en DataAccessException con mensaje comprensible.
- Los Service lanzan BusinessException cuando se rompe una regla. Los formularios atrapan ambas excepciones y muestran un aviso entendible al usuario, nunca el error de SQL.

## 8. Donde vive cada regla de negocio
| Regla | Clase | Detalle |
|---|---|---|
| RN-01, RN-02 | AuthService | Usuario inactivo denegado; la validacion facial debe aprobarse. |
| RN-03 | LaboratorioService | Codigo de laboratorio unico (respaldado por UQ_Laboratorio_codigo). |
| RN-04 | EquipoService.crear | verificarCodigo antes de guardar; traducirErrorEquipo traduce la restriccion UQ_Equipo_codigo a un mensaje comprensible. |
| RN-05, RN-06 | PrestamoService | Al menos un equipo; sin equipos repetidos (respaldado por UQ_Detalle_prestamo_equipo). |
| RN-07, RN-08, RN-14 | PrestamoService | Solo un equipo DISPONIBLE puede prestarse; PRESTADO, DANADO, MANTENIMIENTO y BAJA lo bloquean. |
| RN-09 | PrestamoService | Al confirmar, el equipo pasa a PRESTADO solo si sigue DISPONIBLE. |
| RN-10, RN-11 | DevolucionService | Solo se devuelve un equipo pendiente de un prestamo activo; no se devuelve dos veces. |
| RN-12, RN-13 | DevolucionService | BUENO pasa a DISPONIBLE; DANADO pasa a DANADO. |
| RN-16 | DevolucionService | El prestamo se finaliza solo si no quedan equipos pendientes. |
| RN-15 | MantenimientoService.finalizar | REPARADO deja el equipo DISPONIBLE; si no, BAJA. |
| RN-17 | ReporteService | Vencido = prestamo ACTIVO, con equipos pendientes y fecha esperada anterior a hoy. |
| RN-18 | Todos los Service | Cada operacion relevante registra auditoria en la misma transaccion. |
| RN-19 | PrestamoService | El QR identifica el equipo; no modifica estados. |
| RN-20 | AutorizacionService | Cada Service valida el rol; no depende del menu. |

## 9. Base de datos
El script database/sigelab_schema.sql crea la base SIGELAB con 9 tablas y datos de prueba. El diagrama esta en docs/diagrama_er.md. La devolucion se guarda en columnas de DetallePrestamo, y el estado vencido no se almacena: se calcula con la fecha esperada.

## 10. Pruebas
La matriz de casos CP-01 a CP-18 esta en docs/matriz_de_pruebas.md.
