# Modelo entidad-relacion - SIGELAB

Proyecto Final - Curso 017 Programacion II - UMG

Este diagrama coincide con `database/sigelab_schema.sql` (9 tablas). GitHub dibuja el diagrama automaticamente al abrir este archivo.

```mermaid
erDiagram
    Rol ||--o{ Usuario : "clasifica"
    Laboratorio ||--o{ Equipo : "ubica"
    TipoEquipo ||--o{ Equipo : "categoriza"
    Usuario ||--o{ Prestamo : "registra"
    Prestamo ||--|{ DetallePrestamo : "incluye"
    Equipo ||--o{ DetallePrestamo : "se presta en"
    Usuario |o--o{ DetallePrestamo : "recibe devolucion"
    Equipo ||--o{ Mantenimiento : "tiene"
    Usuario ||--o{ Mantenimiento : "registra"
    Usuario |o--o{ Auditoria : "genera"

    Rol {
        INT id PK
        VARCHAR nombre UK
    }
    Usuario {
        INT id PK
        VARCHAR nombre
        VARCHAR apellido
        VARCHAR username UK
        VARCHAR password_hash
        VARCHAR correo UK
        INT rol_id FK
        VARCHAR estado
    }
    Laboratorio {
        INT id PK
        VARCHAR codigo UK
        VARCHAR nombre
        VARCHAR ubicacion
        INT capacidad
        VARCHAR estado
    }
    TipoEquipo {
        INT id PK
        VARCHAR nombre UK
        VARCHAR descripcion
    }
    Equipo {
        INT id PK
        VARCHAR codigo_inventario UK
        VARCHAR nombre
        INT tipo_equipo_id FK
        VARCHAR marca
        VARCHAR modelo
        VARCHAR numero_serie
        INT laboratorio_id FK
        VARCHAR estado
        VARCHAR observaciones
    }
    Prestamo {
        INT id PK
        VARCHAR solicitante
        DATETIME2 fecha_prestamo
        DATETIME2 fecha_esperada_devolucion
        INT usuario_registra_id FK
        VARCHAR estado
        VARCHAR observaciones
    }
    DetallePrestamo {
        INT id PK
        INT prestamo_id FK
        INT equipo_id FK
        DATETIME2 fecha_devolucion
        VARCHAR condicion_devolucion
        VARCHAR observacion_devolucion
        INT usuario_recibe_id FK
    }
    Mantenimiento {
        INT id PK
        INT equipo_id FK
        DATETIME2 fecha_ingreso
        VARCHAR tipo
        VARCHAR descripcion
        VARCHAR responsable
        DATETIME2 fecha_salida
        VARCHAR resultado
        VARCHAR estado
        VARCHAR observaciones
        INT usuario_id FK
    }
    Auditoria {
        BIGINT id PK
        INT usuario_id FK
        DATETIME2 fecha_hora
        VARCHAR accion
        VARCHAR entidad
        INT id_registro
        VARCHAR descripcion
    }
```

## Notas del modelo
- Leyenda: PK = llave primaria, FK = llave foranea, UK = valor unico (UNIQUE).
- Prestamo y DetallePrestamo: un prestamo tiene uno o mas equipos (RN-05). La restriccion UNIQUE (prestamo_id, equipo_id) impide repetir un equipo en el mismo prestamo (RN-06).
- La devolucion no es una tabla aparte: se guarda en columnas de DetallePrestamo (fecha_devolucion, condicion_devolucion, observacion_devolucion, usuario_recibe_id). Un equipo pendiente tiene fecha y condicion en NULL; uno devuelto tiene ambas (restriccion CK_Detalle_coherencia).
- Auditoria.usuario_id acepta NULL para registrar logins fallidos de usuarios que no existen. Auditoria.id_registro guarda el id del registro afectado sin llave foranea, porque apunta a distintas tablas segun la entidad.
- Estados controlados con CHECK: Usuario y Laboratorio (ACTIVO, INACTIVO), Equipo (DISPONIBLE, PRESTADO, DANADO, MANTENIMIENTO, BAJA), Prestamo (ACTIVO, FINALIZADO), condicion de devolucion (BUENO, DANADO), Mantenimiento (tipo PREVENTIVO o CORRECTIVO; resultado REPARADO o NO_REPARABLE; estado EN_PROCESO o FINALIZADO).
- Un prestamo vencido no se guarda como estado: se calcula cuando fecha_esperada_devolucion ya paso y existen equipos pendientes (RN-17).
- Los rostros del reconocimiento facial no estan en SQL Server: se guardan como archivos en `facial/rostros/<id de usuario>/`.
