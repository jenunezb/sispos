# Clientes de Administración, Facturación y Producción

El arranque ejecuta `src/main/resources/schema.sql` antes de validar Hibernate.
Ese script agrega `correo` y `direccion` automáticamente, sin duplicar clientes ni
cambiar IDs. Las operaciones son idempotentes. No es necesario ejecutar SQL manual
para esta actualización; `scripts/clientes_directorio_general.sql` queda disponible
para una aplicación anticipada opcional. La relación `venta.cliente_id` ya existe.
Se conserva `ddl-auto=validate` y `spring.sql.init.mode=always`.

## API general

Todas estas operaciones requieren `Authorization: Bearer <token>`.

| Método | Ruta | Resultado dentro de `respuesta` |
| --- | --- | --- |
| GET | `/api/clientes` | Lista completa de clientes activos de la empresa, ordenada por nombre, sin paginación |
| POST | `/api/clientes` | Cliente creado con su ID |
| PUT | `/api/clientes/{id}` | Cliente actualizado con su ID |

Roles permitidos: `administrador` (incluido el limitado), `vendedor` y `produccion`.
La empresa se obtiene de la cuenta autenticada, nunca del cuerpo o de parámetros.
El directorio pertenece a la empresa y no se restringe a una sede individual.
Un administrador sin empresa asociada no puede utilizarlo, incluso si es superadmin.
Los clientes eliminados lógicamente desde Producción permanecen inactivos y no
aparecen en el listado ni pueden seleccionarse para nuevas ventas.

Cuerpo de creación y edición:

```json
{
  "nombre": "Comercial Ejemplo SAS",
  "documento": "900123456-7",
  "correo": "compras@ejemplo.com",
  "telefono": "3001234567",
  "direccion": "Calle 10 # 20-30"
}
```

`nombre` es obligatorio y admite hasta 120 caracteres. Los demás campos son
opcionales: `documento` y `telefono` hasta 30 caracteres, `correo` hasta 254 con
formato válido y `direccion` hasta 255. También se acepta `nit` como alias de entrada
de `documento`; la salida conserva `documento`. PUT reemplaza los campos editables:
los opcionales omitidos quedan nulos.

```json
{
  "error": false,
  "respuesta": {
    "id": 7,
    "nombre": "Comercial Ejemplo SAS",
    "telefono": "3001234567",
    "documento": "900123456-7",
    "activo": true,
    "correo": "compras@ejemplo.com",
    "direccion": "Calle 10 # 20-30"
  }
}
```

Los errores usan `{"error":true,"respuesta":"mensaje"}`. La edición de un ID
inexistente, inactivo o ajeno se rechaza sin modificar información.
El frontend puede filtrar el listado localmente por `nombre` o `documento`.

## Ventas

POST `/api/ventas` acepta `clienteId` numérico o `null`, vacío (`""`) u omitido
para consumidor final. El correo del actor se obtiene del token: el valor `correo`
del cuerpo no determina quién vende. Se valida la sede asignada del vendedor y las
sedes autorizadas del administrador limitado. Producción usa su propia sede.

Un cliente seleccionado debe estar activo y pertenecer a la empresa de la sede de
la venta. La validación ocurre antes de descontar inventario y guardar la venta.
Una venta para consumidor final devuelve `cliente: null`.

Las respuestas de ventas conservan el contrato anterior: objeto directo al crear
u obtener una venta y arreglo directo al listar. No se envuelven en `respuesta`,
para mantener compatible el frontend existente. Los endpoints nuevos de clientes
sí usan `MensajeDTO`. Se requiere token también al consultar ventas; el interceptor
del frontend local ya lo agrega. Dentro de cada venta se mantienen `clienteId`
y `clienteNombre` y se agrega `cliente`, con todos los campos del ejemplo anterior.
Los datos reflejan el directorio actual, no una copia histórica inmutable de los
datos fiscales al momento de la venta.

Las rutas `/api/produccion/clientes` y `/api/produccion/ventas` conservan su formato
de respuesta previo para compatibilidad y utilizan las mismas entidades y el mismo
directorio. Los nuevos campos también están disponibles en esas rutas.
Editar por la ruta antigua de Producción conserva correo y dirección cuando esos
campos no se envían, para evitar que formularios anteriores borren información nueva.

## Despliegue

Se fija Java 17 en Gradle, igual que las imágenes del Dockerfile. El build de Docker
ejecuta las pruebas antes de empaquetar `pos-1.0.0.jar`. `railway.toml` configura
el endpoint existente `/` como healthcheck, con un máximo de 120 segundos.
El servidor sigue escuchando en `${PORT:8080}`.

Railway espera HTTP 200 del healthcheck antes de activar el despliegue:
[documentación de Railway](https://docs.railway.com/deployments/healthchecks).
Las variables PostgreSQL existentes deben conservarse, así como los permisos DDL
que el usuario de base de datos ya necesita para ejecutar `schema.sql`.

### Validación realizada el 3 de septiembre de 2026

- `gradlew.bat test bootJar`: 102 pruebas, sin fallos; bytecode Java 17 y migración
  incluida dentro del JAR.
- Arranque real del JAR con Java 17 y PostgreSQL 18 local, con los límites de JVM
  del Dockerfile. Base de prueba independiente con datos ficticios.
- Se simularon clientes preexistentes sin correo ni dirección: `schema.sql` agregó
  las columnas y Hibernate validó el esquema correctamente.
- 39 solicitudes HTTP verificaron los roles, aislamiento entre empresas,
  administrador limitado, validaciones, compatibilidad de Producción, persistencia
  de ventas y cliente opcional. Las operaciones rechazadas no crearon ventas.
- Segundo arranque usando `PORT`: HTTP 200 en `/`, migración repetible y conservación
  de los clientes y ventas de prueba.

No se ha desplegado esta revisión ni se ha modificado la base de Railway. El CLI
no tiene un proyecto vinculado en este directorio y Docker no está instalado aquí;
la ejecución validada es del JAR con Java 17 y PostgreSQL locales. La configuración
y el estado reales de Railway deberán comprobarse al desplegar.
