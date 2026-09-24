# Seguimiento diario de materia prima

El despliegue del backend ejecuta `schema.sql`: agrega la tabla de movimientos y una apertura por existencia actual, sin cambiar cantidades. La apertura se crea una sola vez por inventario y no es una entrada. No se reconstruyen entradas anteriores que no fueron almacenadas.

El frontend muestra la consulta en Sedes → Informes. Endpoints autenticados de administrador, con validación de acceso a la sede:

- `GET /api/materias-primas/historial/{sedeId}`: materias primas disponibles.
- `GET /api/materias-primas/historial/{sedeId}/{materiaId}?desde=2026-09-24&hasta=2026-09-30`: resumen diario y movimientos con fecha y hora de Colombia. No admite fechas futuras ni rangos mayores a 367 días.

Saldo final = inicial + entradas − pérdidas − ventas unitarias − ventas en combo − salidas manuales + ajustes.

Los movimientos conservan el consumo efectivo y el saldo anterior/nuevo; cambios posteriores de receta no alteran el reporte. Los productos cuyo nombre contiene “combo” se clasifican como venta en combo al vender; el resto como unitaria. Las cantidades siempre están en la unidad base del insumo, no necesariamente en unidades de producto vendido.

El primer día se marca parcial, con saldo inicial diario desconocido y apertura exacta aparte. Días anteriores se muestran N/D. Días posteriores sin movimientos arrastran el saldo. El día en curso muestra saldo al corte.

Las cargas iniciales y masivas, entradas/salidas manuales, ventas, complementos, pérdidas y edición de existencia registran movimientos dentro de la transacción. La edición directa se clasifica como ajuste. Un control de versión rechaza actualizaciones concurrentes obsoletas y revierte su historial. Una venta fallida revierte tanto stock como movimientos.

Se conserva el comportamiento existente de anulaciones: anular una venta no repone inventario. Este reporte registra consumo físico, incluyendo el de facturas anuladas cuyo inventario no haya sido reintegrado. La reposición mediante ajuste queda visible por separado.

Desplegar primero backend y después frontend. La apertura usa la existencia real al arrancar la versión nueva (no fija 79 ni modifica stocks). Los cambios de código y scripts directos ajenos a la aplicación que escriban SQL sobre la existencia no pasan por el registro Java.

Validación: pruebas de conciliación diaria, consumo unitario/combo, persistencia JPA de saldo e historial, rollback y suite backend; compilación Angular.
