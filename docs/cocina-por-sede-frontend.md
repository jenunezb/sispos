# Cocina por sede: contrato para frontend

## Estado de la entrega

Implementado en el codigo local. No se desplego ni se modifico la base de datos remota.
La configuracion de cocina es independiente por sede y se utiliza tambien al crear comandas.

## Endpoint recomendado

Todas las solicitudes requieren `Authorization: Bearer <token>`.

Consultar:

```http
GET /api/sedes/15/configuracion/impresion-cocina
```

Activar o desactivar (solo administrador autorizado):

```http
PUT /api/sedes/15/configuracion/impresion-cocina
Content-Type: application/json

{"habilitada": false}
```

Ambos responden HTTP 200 con la misma estructura:

```json
{
  "error": false,
  "respuesta": {
    "sedeId": 15,
    "habilitada": false
  }
}
```

`habilitada` es obligatorio y no admite null. El campo del frontend es
`data.respuesta.habilitada`, no `data.respuesta` (ahora es un objeto).

## Permisos

- Administrador principal de empresa: consulta y actualiza sus sedes.
- Administrador limitado: consulta y actualiza solo sus sedes asignadas.
- Superadministrador: conserva su acceso global existente.
- Vendedor y produccion: consultan exclusivamente su sede asignada; no actualizan.

Se usa el rol del JWT y se comprueba el acceso contra la base de datos. Cambiar
el ID en la URL no permite configurar otra sede sin permisos.

## Integracion de pantalla

1. En el perfil del administrador, seleccionar una sede de las que puede gestionar
   (`GET /api/sedes` ya lista las sedes visibles).
2. Al seleccionar o cambiar sede, consultar el GET y mostrar su valor en el interruptor.
3. Al guardar, enviar el PUT con el ID seleccionado y usar el valor confirmado por el servidor.
4. En facturacion, consultar el GET con la sede actual y usar `respuesta.habilitada`
   para mostrar o habilitar "Imprimir cocina" y, si comparte la opcion, "Enviar a cocina".
5. No conservar una bandera global en localStorage. Si hay cache, identificarla por
   usuario y sede e invalidarla al guardar, cambiar sede o cerrar sesion.
6. Durante la carga o si la consulta falla, no reutilizar el estado de otra sede.
   Deshabilitar temporalmente los botones y mostrar el error. Descartar respuestas
   tardias que correspondan a una sede que ya no esta seleccionada.
7. Recargar la configuracion al entrar en facturacion o recuperar el foco. No hay
   notificacion en tiempo real entre sesiones; el backend siempre verifica la sede
   al crear una comanda, aunque el frontend tenga un valor anterior.

Ejemplo de llamadas con fetch (`token` sin el prefijo Bearer):

```js
async function cocinaPorSede(baseUrl, token, sedeId, habilitada) {
  const actualizar = habilitada !== undefined;
  const response = await fetch(
    `${baseUrl}/api/sedes/${encodeURIComponent(sedeId)}/configuracion/impresion-cocina`,
    {
      method: actualizar ? 'PUT' : 'GET',
      headers: {
        Authorization: `Bearer ${token}`,
        ...(actualizar ? { 'Content-Type': 'application/json' } : {})
      },
      ...(actualizar ? { body: JSON.stringify({ habilitada }) } : {})
    }
  );
  const data = await response.json();
  if (!response.ok || data.error) {
    throw new Error(data.respuesta || 'No se pudo consultar o guardar cocina');
  }
  return data.respuesta; // { sedeId, habilitada }
}
```

Los errores de negocio usan el manejador actual: HTTP 400 con
`{"error":true,"respuesta":"mensaje"}`. El filtro de autenticacion puede devolver
otros estados; comprobar siempre `response.ok` y `data.error`.

## Rutas anteriores

Permanecen disponibles, pero ahora **requieren `?sedeId=15`**:

- GET `/api/administrador/cuentas/{correo}/impresion-cocina?sedeId=15`
- PUT `/api/administrador/cuentas/{correo}/impresion-cocina?sedeId=15`
- GET `/api/vendedor/impresion-cocina?sedeId=15`
- GET `/api/produccion/impresion-cocina?sedeId=15`

Los GET anteriores conservan `{"error":false,"respuesta":true}` (booleano).
El PUT anterior conserva `respuesta` como mensaje de texto; recibe `{"habilitada":true}`.
El correo del endpoint de administrador debe seguir siendo el del administrador autenticado.
Sin sedeId se rechaza la solicitud; no hay actualizacion global ni seleccion implicita.
Coordinar el despliegue con frontend: las llamadas antiguas sin sedeId dejaran de funcionar.

## Datos y despliegue

`schema.sql`, que se ejecuta al iniciar la aplicacion segun su configuracion actual,
agrega `sede.impresion_cocina_habilitada`, copia la preferencia de la empresa a las
sedes pendientes y establece DEFAULT TRUE y NOT NULL. Los siguientes arranques no
sobrescriben los valores ya establecidos. Las nuevas sedes comienzan habilitadas.
La columna antigua de empresa se conserva para la transicion, pero los endpoints
de cocina y la creacion de comandas ya no la usan para decidir el estado.

La migracion no se ejecuto contra una base de datos real durante esta entrega.
No ejecutar simultaneamente instancias antiguas y nuevas que actualicen cocina:
las antiguas siguen usando la configuracion global.

## Verificacion local

Compilacion y 10 pruebas unitarias aprobadas mediante:

```text
gradlew.bat test --tests '*ConfiguracionCocinaSedeServiceTest' --tests '*ComandaCocinaServicioImplTest'
```

Se verifican el aislamiento al actualizar una sede, el rechazo de accesos no
autorizados, la consulta de produccion, la validacion de null y la creacion o
bloqueo de comandas segun la sede aun cuando la empresa tenga el valor contrario.
No se verifico visualmente el frontend porque no esta en este repositorio.
