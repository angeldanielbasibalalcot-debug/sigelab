# Manual de usuario - SIGELAB

Proyecto Final - Curso 017 Programacion II - UMG

SIGELAB permite controlar los equipos de los laboratorios: saber cuales estan disponibles, prestados, danados, en mantenimiento o de baja, y quien tiene cada uno.

## 1. Iniciar sesion
1. Escriba su **Usuario** y su **Contrasena** y pulse **Ingresar**.
2. Si los datos son correctos y el usuario esta activo, el sistema verifica su rostro con la camara. Mire de frente a la camara y espere el resultado.
3. Si el rostro corresponde, se abre el menu principal. Si no, el acceso se deniega.
4. El boton **Salir** cierra la aplicacion.

Casos en los que no se puede entrar: usuario inactivo, contrasena incorrecta, rostro que no corresponde, usuario sin rostro registrado (pida a un administrador que lo registre) o componente facial apagado (avise al administrador para que lo inicie).

## 2. El menu segun el rol
| Menu | Modulos | ADMIN | ENCARGADO | DOCENTE |
|---|---|---|---|---|
| Catalogos | Usuarios, Laboratorios, Tipos de equipo | Si | No | No |
| Catalogos | Equipos | Si | Si | No |
| Operaciones | Prestamos, Devoluciones, Mantenimiento | Si | Si | No |
| Consultas | Reportes | Si | Si | Si |
| Consultas | Auditoria | Si | No | No |

La barra de menus puede mostrar hasta cuatro menus: Archivo (con las opciones Cerrar sesion y Salir), Catalogos, Operaciones y Consultas.

## 3. Dashboard
Muestra cantidades obtenidas de la base de datos: total de equipos, disponibles, prestados, danados, en mantenimiento, de baja, prestamos activos y prestamos vencidos. Pulse **Actualizar** para refrescar los numeros.

## 4. Usuarios (solo ADMIN)
- **Nuevo**: limpia el formulario para crear un usuario. Complete usuario, nombre, apellido, correo, **Contrasena** (obligatoria al crear: si queda vacia aparece el aviso "Ingrese una contrasena.") y rol, y pulse **Guardar**. Si selecciona un usuario existente, el campo cambia a "Nueva contrasena (opcional)": vacio deja la contrasena actual sin cambios.
- Para editar, seleccione un usuario de la tabla, cambie los datos y pulse **Actualizar**. El campo **Usuario** no se puede cambiar al editar.
- La casilla **Activo** es solo informativa: el estado se cambia con el boton **Desactivar**.
- **Desactivar**: el usuario deja de poder iniciar sesion, pero su historial se conserva. Los botones **Desactivar**, **Registrar rostro** y **Eliminar rostro** estan deshabilitados hasta que se selecciona un usuario.
- **Registrar rostro**: seleccione un usuario y pulse el boton; la persona debe mirar a la camara mientras se toman las muestras. Registre el rostro con la misma luz y posicion de camara con que se usara el sistema. Si el usuario no tiene rostro, la pantalla muestra: "Sin rostro registrado: este usuario no podra iniciar sesion."
- **Eliminar rostro**: borra el rostro registrado. Sin rostro, el usuario no podra iniciar sesion.
- **Buscar**: filtra la tabla de usuarios.

## 5. Laboratorios (solo ADMIN)
Campos: ID, Codigo, Nombre, Ubicacion, Capacidad y Activo. **Nuevo**, **Guardar** y **Actualizar** funcionan como en Usuarios. **Desactivar** y **Reactivar** cambian el estado del laboratorio; segun el estado del laboratorio seleccionado, solo uno de los dos botones esta habilitado. El codigo no puede repetirse y la capacidad debe ser mayor que cero. El campo **Buscar laboratorio** filtra la tabla mientras se escribe, sin boton.

## 6. Tipos de equipo (solo ADMIN)
Campos: ID, Nombre y Descripcion, con **Nuevo**, **Guardar** y **Actualizar**. Use **Buscar tipo de equipo** para filtrar. Los tipos se eligen despues desde una lista al registrar equipos.

## 7. Equipos (ADMIN y ENCARGADO)
Campos: Codigo de inventario, Nombre, Tipo de equipo, Marca, Modelo, Numero de serie, Laboratorio, Estado y Observaciones.
- **Nuevo** y **Guardar**: registra un equipo. Un equipo nuevo queda DISPONIBLE. El codigo de inventario no puede repetirse.
- **Actualizar**: guarda los cambios del equipo seleccionado.
- **Recargar**: vuelve a leer la lista desde la base de datos.
- **Buscar**: filtra la tabla.
- **Generar QR**: crea el codigo QR del equipo, que contiene su codigo de inventario.
- **Leer QR**: identifica un equipo a partir de la imagen de su QR y muestra sus datos y estado. Leer un QR no cambia el estado del equipo.

Estados de un equipo: DISPONIBLE (puede prestarse), PRESTADO, DANADO, MANTENIMIENTO y BAJA. Solo un equipo DISPONIBLE puede prestarse.

## 8. Prestamos (ADMIN y ENCARGADO)
1. Escriba el **Solicitante** y la **Fecha esperada** de devolucion (dd/MM/aaaa). La fecha esperada no puede ser anterior a hoy. Las observaciones son opcionales.
2. Agregue equipos de dos maneras: elija uno en **Equipo disponible** y pulse **Agregar**, o use **Identificar por QR** con **Leer QR**.
3. Cada equipo agregado aparece en la tabla; el contador "Equipos en el prestamo" muestra cuantos hay. Use **Quitar seleccionado** si se equivoco.
4. Pulse **Registrar prestamo**. Todos los equipos pasan a PRESTADO.
5. **Limpiar** vacia el formulario.

El sistema rechaza: un prestamo sin equipos, un equipo repetido en el mismo prestamo y cualquier equipo que no este DISPONIBLE.

## 9. Devoluciones (ADMIN y ENCARGADO)
1. La pantalla muestra los equipos pendientes de devolucion (**Actualizar lista** la refresca).
2. Seleccione el equipo que regresa.
3. En **Condicion al regresar** elija BUENO o DANADO y escriba una observacion si hace falta.
4. Pulse **Registrar devolucion del equipo seleccionado**.

Resultado: BUENO deja el equipo DISPONIBLE; DANADO lo deja DANADO. Se puede devolver parte de un prestamo: el prestamo sigue activo mientras quede un equipo pendiente y se finaliza cuando todos fueron devueltos. Un equipo no puede devolverse dos veces.

## 10. Mantenimiento (ADMIN y ENCARGADO)
**Enviar un equipo:** elija el **Equipo** (pueden enviarse equipos DANADOS o DISPONIBLES), el **Tipo de mantenimiento** (preventivo o correctivo), escriba la **Descripcion**, el **Responsable** y las observaciones, y pulse **Enviar a mantenimiento**. El equipo pasa a MANTENIMIENTO y no puede prestarse.

**Cerrar un mantenimiento:** seleccione el mantenimiento en la tabla, elija el **Resultado** y pulse **Finalizar mantenimiento**. El sistema pide una confirmacion (Si / No) antes de cerrarlo. Si fue reparado, el equipo vuelve a DISPONIBLE; si no es reparable, pasa a BAJA.

**Limpiar** vacia el formulario, **Recargar** actualiza la tabla y la casilla **Mostrar solo mantenimientos en proceso** filtra los que siguen abiertos.

## 11. Reportes (todos los roles)
1. Elija el tipo en **Reporte**. Los reportes disponibles son siete: Equipos por estado, Prestamos activos (no vencidos), Prestamos finalizados, Prestamos vencidos, Prestamos entre fechas, Historial de prestamos de un equipo e Historial de mantenimiento.
2. Segun el reporte, se habilitan los filtros **Desde** y **Hasta** (dd/MM/aaaa) o **Codigo de equipo**.
3. Pulse **Consultar**. El contador "Registros" indica cuantos resultados hay. **Limpiar** vacia los filtros.

Un prestamo se considera vencido cuando su fecha esperada ya paso y todavia tiene equipos pendientes.

## 12. Auditoria (solo ADMIN)
Muestra quien hizo cada operacion importante: inicios de sesion, prestamos, devoluciones, mantenimientos, desactivacion de usuarios y registro o eliminacion de rostros. Filtre por **Accion** y por fechas **Desde** y **Hasta** (dd/MM/yyyy), y pulse **Consultar**. **Limpiar** quita los filtros.

## 13. Ejemplo completo del ciclo de un equipo
1. En **Equipos**, busque CAM-001 y compruebe que esta DISPONIBLE.
2. En **Prestamos**, cree un prestamo con CAM-001 y registrelo.
3. Intente prestar CAM-001 otra vez: el sistema lo rechaza.
4. En **Devoluciones**, devuelvala con condicion DANADO.
5. Intente prestarla: el sistema la rechaza porque esta DANADA.
6. En **Mantenimiento**, envie CAM-001 a mantenimiento.
7. Finalice el mantenimiento como reparado.
8. En **Equipos**, compruebe que vuelve a DISPONIBLE.
9. Cree un nuevo prestamo con CAM-001.

## 14. Mensajes frecuentes
El sistema nunca muestra errores tecnicos de base de datos: si algo no se puede hacer, un aviso explica el motivo (por ejemplo, equipo no disponible, codigo repetido o campo obligatorio vacio). Si ve un aviso generico de error inesperado, repita la operacion y, si persiste, avise al administrador.