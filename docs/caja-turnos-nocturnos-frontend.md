# Entrega backend: caja por turno y continuidad después de medianoche

## Resultado

El resumen actual de caja ya no se reinicia a las 00:00 cuando existe una caja abierta.
Las ventas y los gastos se calculan desde `fechaApertura` hasta la hora actual, aunque el
turno atraviese uno o más cambios de fecha. El periodo queda cerrado únicamente al ejecutar
el cierre de caja.

## Endpoints consumidos por el frontend

No cambian las rutas, parámetros ni estructuras de respuesta existentes:

- `GET /api/administrador/balance/general`
- `GET /api/administrador/balance/sedes`
- `GET /api/vendedor/sede?correo={correo}`
- `GET /api/administrador/cajas/actual?sedeId={sedeId}`
- Endpoints existentes de apertura y cierre de caja.

Cuando los endpoints de balance se consultan **sin** `desde` y `hasta`, el backend usa la
apertura de la caja activa de cada sede. Si no existe una caja abierta, conserva como
compatibilidad el comportamiento diario actual.

Cuando se envían `desde` y `hasta`, el backend respeta el rango solicitado. Los reportes
históricos por fechas no cambiaron.

## Cambios requeridos en frontend

No hay cambios obligatorios. El frontend puede seguir consumiendo los mismos campos y
endpoints. Después de medianoche debe continuar consultando normalmente el resumen; recibirá
el acumulado completo del turno.

Como mejora opcional, la pantalla puede consultar `/api/administrador/cajas/actual` y mostrar:

- estado de la caja;
- fecha y hora de apertura;
- identificador del turno;
- texto "Acumulado desde la apertura".

## Ejemplo funcional

Para una caja abierta el 15 de agosto a las 19:30 y todavía abierta el 16 de agosto a las
02:00, el resumen incluye todas las ventas y gastos entre esos dos momentos. No comienza de
nuevo a las 00:00.

## Consideraciones

- Apertura, cierre y cálculo del turno utilizan la zona `America/Bogota`.
- No hubo cambios de base de datos ni migraciones.
- No se modificaron los DTO ni el formato JSON entregado al frontend.
- No se modificó el comportamiento de filtros históricos explícitos.

## Validación

Se agregaron pruebas automáticas para un turno nocturno tanto en el balance del administrador
como en el resumen del vendedor. La suite completa del backend compila y pasa sin errores.
