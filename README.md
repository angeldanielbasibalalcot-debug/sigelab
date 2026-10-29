# SIGELAB - Sistema de Gestion y Control de Laboratorios Universitarios

Proyecto Final - Curso 017 Programacion II - Universidad Mariano Galvez de Guatemala (UMG)
Catedratico: Ing. Alejandro Diaz

## Integrantes
- Angel Daniel Basibal Alcot: login y usuarios, reconocimiento facial, mantenimiento, auditoria, prestamos, devoluciones, QR en prestamos, dashboard y reportes.
- Christian David Chacon Icu: equipos y generacion de codigos QR.
- Edgar Joel Castro Ramirez: version inicial de prestamos y devoluciones.
- Alexander Adolfo De La Cruz Taperia: integrante del equipo.

## Requisitos
- Java 21 (Temurin recomendado)
- Maven (incluido en IntelliJ IDEA)
- Microsoft SQL Server 2022 (puede usarse con Docker)
- Python 3.12 (componente de reconocimiento facial)
- Una camara web (en el equipo de desarrollo se uso Iriun Webcam)

## Base de datos
1. Levantar SQL Server con Docker (cambiar la contrasena por una propia):
   ```
   docker run -e "ACCEPT_EULA=Y" -e "MSSQL_SA_PASSWORD=<TU_CONTRASENA>" -p 1433:1433 --name sqlserver -d mcr.microsoft.com/mssql/server:2022-latest
   ```
2. Conectarse con SSMS (`localhost,1433`, usuario `sa`).
3. Ejecutar `database/sigelab_schema.sql`. El script crea la base de datos SIGELAB, las tablas y los datos de prueba (roles, usuarios, tipos de equipo, laboratorios y equipos).

## Configuracion de conexion
1. Copiar `config.properties.example` como `config.properties` (raiz del proyecto).
2. Cambiar `db.password` por la contrasena de SQL Server y ajustar `db.url` si es necesario.
3. `config.properties` no se sube a Git.

## Ejecutar la aplicacion
1. Iniciar primero el componente facial (ver la seccion siguiente).
2. Abrir el proyecto en IntelliJ IDEA y esperar a que Maven descargue las dependencias.
3. Ejecutar la clase `com.umg.sigelab.Main`. Se abre la pantalla de inicio de sesion.

## Componente de reconocimiento facial
El login exige usuario, contrasena y validacion facial (RN-02). La validacion la hace una API local en Python (Flask + OpenCV, algoritmo LBPH) que escucha en `127.0.0.1:5001`.

Preparacion (una sola vez), en PowerShell desde la raiz del proyecto:
```
python -m venv facial\.venv
facial\.venv\Scripts\python.exe -m pip install -r facial\requirements.txt
```

Iniciar la API (antes de abrir la aplicacion; la terminal queda ocupada):
```
$env:SIGELAB_CAMARA = "1"
facial\.venv\Scripts\python.exe facial\api_facial.py
```

Variables de entorno opcionales:
| Variable | Significado | Valor por defecto |
|---|---|---|
| SIGELAB_CAMARA | Indice de la camara a usar | 0 |
| SIGELAB_PUERTO | Puerto de la API | 5001 |
| SIGELAB_UMBRAL | Distancia maxima para aceptar un rostro (menor es mas estricto) | 50 |

Registrar rostros: iniciar sesion como administrador, abrir el modulo Usuarios, seleccionar un usuario y usar el boton Registrar rostro (se toman 20 muestras). Un usuario sin rostro registrado no puede iniciar sesion. Registrar y verificar con la misma luz y la misma posicion de camara mejora el reconocimiento. Las fotos se guardan en `facial/rostros/` y no se suben a Git.

## Usuarios de prueba
| Usuario | Contrasena | Rol |
|---|---|---|
| admin | Admin123 | ADMIN |
| encargado | Encar123 | ENCARGADO |
| docente | Docen123 | DOCENTE |
| inactivo | Inact123 | ENCARGADO (INACTIVO) |

Todos necesitan un rostro registrado para entrar, excepto el usuario inactivo, que siempre es rechazado (RN-01).

## Funciones principales
- Inicio de sesion con usuario, contrasena y validacion facial.
- Usuarios y roles (ADMIN, ENCARGADO, DOCENTE) con desactivacion logica.
- Laboratorios, tipos de equipo y equipos con codigos unicos y cinco estados (DISPONIBLE, PRESTADO, DANADO, MANTENIMIENTO, BAJA).
- Codigo QR por equipo y lectura de QR para agregar equipos a un prestamo.
- Prestamos de varios equipos (encabezado y detalle) con cambio automatico de estados.
- Devoluciones parciales o completas, indicando si el equipo regresa BUENO o DANADO.
- Mantenimiento preventivo y correctivo, con cierre como reparado (DISPONIBLE) o no reparable (BAJA).
- Auditoria de acciones relevantes.
- Dashboard con cantidades y reportes con filtros por estado y fechas.

## Arquitectura
View -> Controller -> Service -> Repository -> JDBC -> SQL Server

- `view`: formularios Swing (sin SQL)
- `controller`: coordina la interfaz con los servicios
- `service`: reglas de negocio (RN-01 a RN-20)
- `repository`: consultas SQL
- `model`: entidades y enums
- `exception`: BusinessException y DataAccessException
- `util`: conexion, QR, cliente facial y sesion actual

## Dependencias externas
- SQL Server 2022 y el driver JDBC mssql-jdbc 12.8.1.jre11.
- ZXing (core y javase 3.5.3) para generar y leer codigos QR.
- Gson 2.11.0 para el manejo de JSON.
- forms_rt 7.0.3 (runtime de los formularios .form del GUI Designer de IntelliJ).
- Python 3.12 con Flask, opencv-contrib-python 4.10.0.84 y numpy menor a 2.1 (ver `facial/requirements.txt`).

## Flujo de trabajo en Git
- No se trabaja directamente sobre `master`: una rama por funcionalidad (`feat/...`, `fix/...`, `docs/...`).
- Los cambios llegan a `master` mediante Pull Request.
- Mensajes de commit: `feat:`, `fix:`, `docs:`, `chore:`.
