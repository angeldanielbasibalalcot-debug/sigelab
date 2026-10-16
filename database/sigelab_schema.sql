-- =====================================================
-- SIGELAB - Script de base de datos (SQL Server)
-- Curso 017 Programacion II - UMG
-- =====================================================
IF DB_ID('SIGELAB') IS NULL
    CREATE DATABASE SIGELAB;
GO
USE SIGELAB;
GO

-- ---------- LIMPIEZA (para poder re-ejecutar el script) ----------
IF OBJECT_ID('Auditoria') IS NOT NULL DROP TABLE Auditoria;
IF OBJECT_ID('Mantenimiento') IS NOT NULL DROP TABLE Mantenimiento;
IF OBJECT_ID('DetallePrestamo') IS NOT NULL DROP TABLE DetallePrestamo;
IF OBJECT_ID('Prestamo') IS NOT NULL DROP TABLE Prestamo;
IF OBJECT_ID('Equipo') IS NOT NULL DROP TABLE Equipo;
IF OBJECT_ID('TipoEquipo') IS NOT NULL DROP TABLE TipoEquipo;
IF OBJECT_ID('Laboratorio') IS NOT NULL DROP TABLE Laboratorio;
IF OBJECT_ID('Usuario') IS NOT NULL DROP TABLE Usuario;
IF OBJECT_ID('Rol') IS NOT NULL DROP TABLE Rol;
GO

-- ---------- ROL ----------
CREATE TABLE Rol (
    id      INT IDENTITY(1,1) CONSTRAINT PK_Rol PRIMARY KEY,
    nombre  VARCHAR(30) NOT NULL CONSTRAINT UQ_Rol_nombre UNIQUE
);

-- ---------- USUARIO ----------
CREATE TABLE Usuario (
    id             INT IDENTITY(1,1) CONSTRAINT PK_Usuario PRIMARY KEY,
    nombre         VARCHAR(60)  NOT NULL,
    apellido       VARCHAR(60)  NOT NULL,
    username       VARCHAR(40)  NOT NULL CONSTRAINT UQ_Usuario_username UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    correo         VARCHAR(100) NOT NULL CONSTRAINT UQ_Usuario_correo UNIQUE,
    rol_id         INT NOT NULL CONSTRAINT FK_Usuario_Rol REFERENCES Rol(id),
    estado         VARCHAR(10) NOT NULL CONSTRAINT DF_Usuario_estado DEFAULT 'ACTIVO',
    CONSTRAINT CK_Usuario_estado CHECK (estado IN ('ACTIVO','INACTIVO'))
);

-- ---------- LABORATORIO ----------
CREATE TABLE Laboratorio (
    id         INT IDENTITY(1,1) CONSTRAINT PK_Laboratorio PRIMARY KEY,
    codigo     VARCHAR(20)  NOT NULL CONSTRAINT UQ_Laboratorio_codigo UNIQUE,
    nombre     VARCHAR(100) NOT NULL,
    ubicacion  VARCHAR(150) NOT NULL,
    capacidad  INT NOT NULL,
    estado     VARCHAR(10) NOT NULL CONSTRAINT DF_Laboratorio_estado DEFAULT 'ACTIVO',
    CONSTRAINT CK_Laboratorio_capacidad CHECK (capacidad > 0),
    CONSTRAINT CK_Laboratorio_estado CHECK (estado IN ('ACTIVO','INACTIVO'))
);

-- ---------- TIPO EQUIPO ----------
CREATE TABLE TipoEquipo (
    id           INT IDENTITY(1,1) CONSTRAINT PK_TipoEquipo PRIMARY KEY,
    nombre       VARCHAR(50) NOT NULL CONSTRAINT UQ_TipoEquipo_nombre UNIQUE,
    descripcion  VARCHAR(200) NULL
);

-- ---------- EQUIPO ----------
CREATE TABLE Equipo (
    id                 INT IDENTITY(1,1) CONSTRAINT PK_Equipo PRIMARY KEY,
    codigo_inventario  VARCHAR(30)  NOT NULL CONSTRAINT UQ_Equipo_codigo UNIQUE,
    nombre             VARCHAR(100) NOT NULL,
    tipo_equipo_id     INT NOT NULL CONSTRAINT FK_Equipo_Tipo REFERENCES TipoEquipo(id),
    marca              VARCHAR(50) NULL,
    modelo             VARCHAR(50) NULL,
    numero_serie       VARCHAR(80) NULL,
    laboratorio_id     INT NOT NULL CONSTRAINT FK_Equipo_Laboratorio REFERENCES Laboratorio(id),
    estado             VARCHAR(15) NOT NULL CONSTRAINT DF_Equipo_estado DEFAULT 'DISPONIBLE',
    observaciones      VARCHAR(300) NULL,
    CONSTRAINT CK_Equipo_estado CHECK (estado IN ('DISPONIBLE','PRESTADO','DANADO','MANTENIMIENTO','BAJA'))
);

-- ---------- PRESTAMO (encabezado) ----------
CREATE TABLE Prestamo (
    id                         INT IDENTITY(1,1) CONSTRAINT PK_Prestamo PRIMARY KEY,
    solicitante                VARCHAR(120) NOT NULL,
    fecha_prestamo             DATETIME2 NOT NULL CONSTRAINT DF_Prestamo_fecha DEFAULT SYSDATETIME(),
    fecha_esperada_devolucion  DATETIME2 NOT NULL,
    usuario_registra_id        INT NOT NULL CONSTRAINT FK_Prestamo_Usuario REFERENCES Usuario(id),
    estado                     VARCHAR(12) NOT NULL CONSTRAINT DF_Prestamo_estado DEFAULT 'ACTIVO',
    observaciones              VARCHAR(300) NULL,
    CONSTRAINT CK_Prestamo_estado CHECK (estado IN ('ACTIVO','FINALIZADO')),
    CONSTRAINT CK_Prestamo_fechas CHECK (fecha_esperada_devolucion >= fecha_prestamo)
);

-- ---------- DETALLE PRESTAMO (equipos del prestamo + su devolucion) ----------
CREATE TABLE DetallePrestamo (
    id                      INT IDENTITY(1,1) CONSTRAINT PK_DetallePrestamo PRIMARY KEY,
    prestamo_id             INT NOT NULL CONSTRAINT FK_Detalle_Prestamo REFERENCES Prestamo(id),
    equipo_id               INT NOT NULL CONSTRAINT FK_Detalle_Equipo REFERENCES Equipo(id),
    fecha_devolucion        DATETIME2 NULL,
    condicion_devolucion    VARCHAR(10) NULL,
    observacion_devolucion  VARCHAR(300) NULL,
    usuario_recibe_id       INT NULL CONSTRAINT FK_Detalle_UsuarioRecibe REFERENCES Usuario(id),
    -- RN-06: el mismo equipo no puede repetirse en el mismo prestamo
    CONSTRAINT UQ_Detalle_prestamo_equipo UNIQUE (prestamo_id, equipo_id),
    CONSTRAINT CK_Detalle_condicion CHECK (condicion_devolucion IS NULL OR condicion_devolucion IN ('BUENO','DANADO')),
    -- Devuelto = fecha y condicion juntas; pendiente = ambas NULL
    CONSTRAINT CK_Detalle_coherencia CHECK (
        (fecha_devolucion IS NULL AND condicion_devolucion IS NULL)
        OR (fecha_devolucion IS NOT NULL AND condicion_devolucion IS NOT NULL)
    )
);

-- ---------- MANTENIMIENTO ----------
CREATE TABLE Mantenimiento (
    id               INT IDENTITY(1,1) CONSTRAINT PK_Mantenimiento PRIMARY KEY,
    equipo_id        INT NOT NULL CONSTRAINT FK_Mant_Equipo REFERENCES Equipo(id),
    fecha_ingreso    DATETIME2 NOT NULL CONSTRAINT DF_Mant_ingreso DEFAULT SYSDATETIME(),
    tipo             VARCHAR(12) NOT NULL,
    descripcion      VARCHAR(300) NOT NULL,
    responsable      VARCHAR(100) NOT NULL,
    fecha_salida     DATETIME2 NULL,
    resultado        VARCHAR(15) NULL,
    estado           VARCHAR(12) NOT NULL CONSTRAINT DF_Mant_estado DEFAULT 'EN_PROCESO',
    observaciones    VARCHAR(300) NULL,
    usuario_id       INT NOT NULL CONSTRAINT FK_Mant_Usuario REFERENCES Usuario(id),
    CONSTRAINT CK_Mant_tipo CHECK (tipo IN ('PREVENTIVO','CORRECTIVO')),
    CONSTRAINT CK_Mant_resultado CHECK (resultado IS NULL OR resultado IN ('REPARADO','NO_REPARABLE')),
    CONSTRAINT CK_Mant_estado CHECK (estado IN ('EN_PROCESO','FINALIZADO')),
    CONSTRAINT CK_Mant_fechas CHECK (fecha_salida IS NULL OR fecha_salida >= fecha_ingreso)
);

-- ---------- AUDITORIA ----------
CREATE TABLE Auditoria (
    id           BIGINT IDENTITY(1,1) CONSTRAINT PK_Auditoria PRIMARY KEY,
    usuario_id   INT NULL CONSTRAINT FK_Auditoria_Usuario REFERENCES Usuario(id), -- NULL si el login fallido no corresponde a un usuario
    fecha_hora   DATETIME2 NOT NULL CONSTRAINT DF_Auditoria_fecha DEFAULT SYSDATETIME(),
    accion       VARCHAR(50) NOT NULL,
    entidad      VARCHAR(50) NOT NULL,
    id_registro  INT NULL,
    descripcion  VARCHAR(400) NULL
);
GO

-- ---------- INDICES utiles para reportes ----------
CREATE INDEX IX_Equipo_estado ON Equipo(estado);
CREATE INDEX IX_Prestamo_fecha ON Prestamo(fecha_prestamo);
CREATE INDEX IX_Detalle_equipo ON DetallePrestamo(equipo_id);
CREATE INDEX IX_Auditoria_fecha ON Auditoria(fecha_hora);
GO

-- =====================================================
-- DATOS DE PRUEBA
-- (password_hash = SHA-256 en hexadecimal minusculas.
--  Si luego usan BCrypt, actualicen estos valores.)
-- =====================================================
INSERT INTO Rol (nombre) VALUES ('ADMIN'), ('ENCARGADO'), ('DOCENTE');

INSERT INTO Usuario (nombre, apellido, username, password_hash, correo, rol_id, estado) VALUES
('Admin',  'Sistema',  'admin',     LOWER(CONVERT(VARCHAR(64), HASHBYTES('SHA2_256','Admin123'),2)),  'admin@umg.edu.gt',     1, 'ACTIVO'),
('Carlos', 'Perez',    'encargado', LOWER(CONVERT(VARCHAR(64), HASHBYTES('SHA2_256','Encar123'),2)),  'encargado@umg.edu.gt',  2, 'ACTIVO'),
('Alejandro',  'Diaz',    'docente',   LOWER(CONVERT(VARCHAR(64), HASHBYTES('SHA2_256','Docen123'),2)),  'docente@umg.edu.gt',    3, 'ACTIVO'),
('Juan',   'Inactivo', 'inactivo',  LOWER(CONVERT(VARCHAR(64), HASHBYTES('SHA2_256','Inact123'),2)),  'inactivo@umg.edu.gt',   2, 'INACTIVO');

INSERT INTO TipoEquipo (nombre, descripcion) VALUES
('Laptop','Computadoras portatiles'),
('Camara','Camaras fotograficas y de video'),
('Proyector','Proyectores multimedia'),
('Router','Equipos de red - router'),
('Switch','Equipos de red - switch'),
('Tablet','Tablets'),
('Monitor','Monitores'),
('Kit electronico','Kits de electronica y robotica');

INSERT INTO Laboratorio (codigo, nombre, ubicacion, capacidad, estado) VALUES
('LAB-01','Laboratorio de Computacion 1','Edificio A, nivel 1', 30, 'ACTIVO'),
('LAB-02','Laboratorio de Redes','Edificio B, nivel 2', 25, 'ACTIVO'),
('LAB-03','Laboratorio de Electronica','Edificio B, nivel 1', 20, 'ACTIVO');

INSERT INTO Equipo (codigo_inventario, nombre, tipo_equipo_id, marca, modelo, numero_serie, laboratorio_id, estado, observaciones) VALUES
('CAM-001','Camara Canon',        2,'Canon','EOS 250D','SN-CAM-001',1,'DISPONIBLE',NULL),
('CAM-002','Camara Sony',         2,'Sony','ZV-E10','SN-CAM-002',1,'DISPONIBLE',NULL),
('LAP-001','Laptop Dell',         1,'Dell','Latitude 5420','SN-LAP-001',1,'DISPONIBLE',NULL),
('LAP-002','Laptop HP',           1,'HP','ProBook 450','SN-LAP-002',1,'DISPONIBLE',NULL),
('PRO-001','Proyector Epson',     3,'Epson','X49','SN-PRO-001',1,'DISPONIBLE',NULL),
('ROU-001','Router Cisco',        4,'Cisco','ISR 1100','SN-ROU-001',2,'DISPONIBLE',NULL),
('SWI-001','Switch TP-Link',      5,'TP-Link','TL-SG108','SN-SWI-001',2,'DISPONIBLE',NULL),
('TAB-001','Tablet Samsung',      6,'Samsung','Tab A8','SN-TAB-001',1,'DISPONIBLE',NULL),
('MON-001','Monitor LG',          7,'LG','24MK430H','SN-MON-001',1,'DISPONIBLE',NULL),
('KIT-001','Kit Arduino',         8,'Arduino','Starter Kit','SN-KIT-001',3,'DISPONIBLE',NULL),
('LAP-003','Laptop Lenovo (baja)',1,'Lenovo','ThinkPad T440','SN-LAP-003',1,'BAJA','Pantalla irreparable'),
('CAM-003','Camara GoPro (danada)',2,'GoPro','Hero 9','SN-CAM-003',1,'DANADO','Lente roto'),
('PRO-002','Proyector BenQ (mant.)',3,'BenQ','MS550','SN-PRO-002',1,'MANTENIMIENTO','Cambio de lampara');
GO

-- Verificacion rapida
SELECT estado, COUNT(*) AS total FROM Equipo GROUP BY estado;
GO
