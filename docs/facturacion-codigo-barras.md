# Facturación por código de barras

## Puesta en marcha

1. En PostgreSQL, ejecutar src/main/resources/db/migration/V20261111_01__producto_codigo_barras.sql antes de iniciar el backend actualizado. La aplicación usa ddl-auto=validate y no incluye un ejecutor Flyway: copiar el SQL al repositorio no modifica la base de datos.
2. Desplegar backend y frontend juntos.
3. En Productos, crear o editar el código de barras. Es opcional, texto de hasta 100 caracteres; conserva ceros iniciales. Un código solo puede pertenecer a un producto de cada empresa, incluso si está desactivado. Para reutilizarlo hay que quitarlo del producto anterior.
4. En Facturación, seleccionar el campo Agregar por código de barras y escanear con un lector USB/Bluetooth que escriba como teclado y finalice con Enter. También se puede escribir el código y usar Agregar.

Cada lectura agrega una unidad al carrito usando el flujo existente, con selección de complementos cuando corresponde. Si hay una selección o consulta de complementos pendiente, terminarla y volver a pulsar Enter; se conserva el código pendiente. No incluye lectura por cámara ni generación de etiquetas.

La coincidencia es exacta sobre el inventario cargado de la sede. Un código inexistente o sin stock muestra un mensaje. La venta sigue usando los identificadores internos y las validaciones del backend. Después de cambiar códigos en el catálogo, recargar facturación.

## API y datos

- ProductoCrearDTO, ProductoActualizarDTO y ProductoDTO incluyen codigoBarras opcional.
- InventarioDTO devuelve codigoBarras tanto en el listado de inventario como en la proyección de inventario completo.
- Un código vacío se guarda como NULL. Se recortan espacios exteriores y se rechazan caracteres de control.
- El índice único (empresa_id, codigo_barras) protege también ante escrituras simultáneas. Diferentes empresas pueden usar el mismo código.
- El campo usa las rutas existentes de productos e inventario; no requiere una consulta de red por cada lectura.

## Verificación manual con lector

Crear un producto con código 00123 y stock 2; escanear dos veces y comprobar cantidad 2 y total. La tercera lectura debe avisar que no hay más stock. Probar un código desconocido, uno de otra sede y un producto con complementos. Guardar la venta y comprobar el descuento de inventario habitual.
