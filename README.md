# SIGELAB - Sistema de Gestion y Control de Laboratorios Universitarios

Proyecto Final - Curso 017 Programacion II - UMG

## Integrantes
- (completar)

## Requisitos
- Java 21 (Temurin recomendado)
- Maven (incluido en IntelliJ IDEA)
- Microsoft SQL Server 2022 (puede usarse con Docker)
- Python 3 (componente de reconocimiento facial)

## Base de datos
1. Levantar SQL Server con Docker:
   ```
   docker run -e "ACCEPT_EULA=Y" -e "MSSQL_SA_PASSWORD=Sigelab#2026" -p 1433:1433 --name sigelab-sql -d mcr.microsoft.com/mssql/server:2022-latest
   ```
2. Conectarse con SSMS (`localhost,1433`, usuario `sa`).
3. Ejecutar `database/sigelab_schema.sql`.

## Configuracion de conexion
1. Copiar `config.properties.example` como `config.properties` (raiz del proyecto).
2. Ajustar `db.password` y demas datos.

## Ejecutar
Ejecutar la clase `com.umg.sigelab.Main` desde IntelliJ. Por ahora solo prueba la conexion.

## Usuarios de prueba
| Usuario | Contrasena | Rol |
|---|---|---|
| admin | Admin123 | ADMIN |
| encargado | Encar123 | ENCARGADO |
| docente | Docen123 | DOCENTE |
| inactivo | Inact123 | ENCARGADO (INACTIVO) |

## Arquitectura
View -> Controller -> Service -> Repository -> JDBC -> SQL Server

- `view`: formularios Swing (sin SQL)
- `controller`: coordina la interfaz con los servicios
- `service`: reglas de negocio (RN-01 a RN-20)
- `repository`: consultas SQL
- `model`: entidades y enums
- `exception`: BusinessException y DataAccessException
- `util`: conexion, QR, cliente facial

## Flujo de trabajo en Git
- No trabajar directamente sobre `master`: una rama por funcionalidad (`feat/...`, `fix/...`).
- Pull Request revisado por otro integrante antes de hacer merge.
- Mensajes de commit: `feat:`, `fix:`, `docs:`, `chore:`.
