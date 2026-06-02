# Entrega Backend: Gastos Diarios Integrados al Balance

Fecha: 2026-06-02

## Objetivo

Se implementó en backend el manejo de gastos diarios para que el balance de ventas refleje también los egresos de caja del día y permita que el cuadre de caja coincida con el dinero real disponible.

Casos cubiertos:

- Compra de verduras o insumos pagados el mismo día.
- Pago de recibos o servicios.
- Cualquier otro gasto operativo de la sede.

## Resultado Funcional

Antes:

- El balance sólo contemplaba ventas, costo de producción, inventario y cantidad de ventas.
- No existía un registro de egresos de caja manuales.
- El cuadre podía diferir del efectivo real porque los gastos no estaban descontados.

Ahora:

- Se puede registrar un gasto diario por sede.
- Cada gasto queda asociado a:
  - sede
  - descripción
  - valor
  - modo de pago
  - fecha de registro
  - administrador que lo registró
- El balance general y el balance por sede incluyen esos gastos.
- El balance calcula la caja esperada teniendo en cuenta gastos pagados en efectivo.

## Modelo Implementado

Se creó la entidad `GastoDiario` con los siguientes campos:

- `id`
- `sedeId`
- `administradorId`
- `descripcion`
- `valor`
- `fecha`
- `modoPago`

Tabla creada:

- `gasto_diario`

## Endpoints Nuevos

### 1. Crear gasto

`POST /api/administrador/gastos`

Header requerido:

`Authorization: Bearer <token>`

Body:

```json
{
  "sedeId": 1,
  "descripcion": "Compra de verduras",
  "valor": 45000,
  "modoPago": "EFECTIVO"
}
```

Reglas:

- `sedeId` obligatorio
- `descripcion` obligatoria
- `valor` obligatorio y mayor a `0`
- `modoPago` obligatorio
- El administrador autenticado debe tener acceso a la sede

Respuesta esperada:

```json
{
  "error": false,
  "respuesta": {
    "id": 10,
    "sedeId": 1,
    "sedeNombre": "Sede Centro",
    "descripcion": "Compra de verduras",
    "valor": 45000,
    "modoPago": "EFECTIVO",
    "fecha": "2026-06-02T14:32:10",
    "administradorId": 7,
    "administradorNombre": "Juan Perez"
  }
}
```

### 2. Listar gastos

`GET /api/administrador/gastos`

Query params soportados:

- `empresaNit` opcional
- `sedeId` opcional
- `desde` opcional en formato `YYYY-MM-DD`
- `hasta` opcional en formato `YYYY-MM-DD`

Ejemplo:

`GET /api/administrador/gastos?sedeId=1&desde=2026-06-02&hasta=2026-06-02`

Comportamiento:

- Si no se envían fechas, devuelve los gastos del día actual.
- Si el usuario es administrador delegado, sólo verá gastos de las sedes permitidas.

## Endpoints Existentes Modificados

### 1. Balance general

`GET /api/administrador/balance/general`

### 2. Balance por sedes

`GET /api/administrador/balance/sedes`

Ambos endpoints ahora incluyen campos adicionales de gastos y caja.

## Nuevos Campos del Balance

### En balance general

Campos actuales del response:

- `totalVentas`
- `costoProduccion`
- `utilidadBruta`
- `totalGastos`
- `gastosEfectivo`
- `gastosTransferencia`
- `cajaEsperada`
- `utilidadNeta`
- `valorInventario`
- `stockTotal`
- `cantidadVentas`
- `ventasEfectivo`
- `ventasTransferencia`

Ejemplo:

```json
{
  "totalVentas": 500000,
  "costoProduccion": 200000,
  "utilidadBruta": 300000,
  "totalGastos": 50000,
  "gastosEfectivo": 30000,
  "gastosTransferencia": 20000,
  "cajaEsperada": 170000,
  "utilidadNeta": 250000,
  "valorInventario": 800000,
  "stockTotal": 120,
  "cantidadVentas": 35,
  "ventasEfectivo": 200000,
  "ventasTransferencia": 300000
}
```

### En balance por sede

Cada sede ahora devuelve además:

- `totalGastos`
- `gastosEfectivo`
- `gastosTransferencia`
- `cajaEsperada`
- `utilidadNeta`

## Reglas de Negocio Aplicadas

Fórmulas implementadas:

- `utilidadBruta = totalVentas - costoProduccion`
- `utilidadNeta = utilidadBruta - totalGastos`
- `cajaEsperada = ventasEfectivo - gastosEfectivo`

Nota importante:

- Sólo los gastos pagados en `EFECTIVO` afectan `cajaEsperada`.
- Los gastos pagados en `TRANSFERENCIA` afectan la utilidad, pero no el efectivo físico en caja.

## Qué Debe Hacer Front

## 1. Crear formulario de gastos

Se necesita una pantalla o modal para registrar gastos con estos campos:

- sede
- descripción
- valor
- modo de pago

Valores permitidos en `modoPago`:

- `EFECTIVO`
- `TRANSFERENCIA`

## 2. Consumir endpoint de creación

Al guardar el formulario, llamar:

`POST /api/administrador/gastos`

Con payload:

```json
{
  "sedeId": 1,
  "descripcion": "Pago recibo de energia",
  "valor": 120000,
  "modoPago": "TRANSFERENCIA"
}
```

## 3. Consumir listado de gastos

Si el front requiere historial o tabla de gastos:

`GET /api/administrador/gastos?sedeId=1&desde=2026-06-01&hasta=2026-06-30`

## 4. Actualizar vista de balance

En la UI de balance deben mostrarse mínimo:

- total ventas
- ventas en efectivo
- ventas por transferencia
- costo de producción
- total gastos
- gastos en efectivo
- gastos por transferencia
- caja esperada
- utilidad bruta
- utilidad neta

## 5. Actualizar cuadre de caja

Si el front tiene un módulo de cuadre, el valor de referencia para caja debe tomar:

`cajaEsperada`

No debe seguir usando únicamente:

`ventasEfectivo`

porque ahora el efectivo real del día debe descontar los gastos pagados en efectivo.

## Recomendación de UX

Para evitar errores operativos:

- Mostrar el modo de pago como selector obligatorio.
- Mostrar confirmación al registrar el gasto.
- Refrescar balance al guardar gasto.
- Refrescar listado de gastos al guardar gasto.
- Mostrar el detalle de quién registró el gasto y la fecha.

## Resumen Técnico de Cambios Realizados

Backend implementado:

- Nueva entidad `GastoDiario`
- Nuevo repositorio `GastoDiarioRepository`
- Nuevo servicio `GastoDiarioServicio`
- Nuevo controlador `GastoDiarioController`
- Nueva tabla `gasto_diario`
- Ajuste de `BalanceGeneralDTO`
- Ajuste de `BalanceSedeDTO`
- Ajuste de `BalanceServicioImpl`
- Ajuste de `BalanceController`
- Ajuste de pruebas unitarias del balance

## Validación Realizada

Se ejecutaron pruebas del backend:

- `./gradlew test`

Resultado:

- pruebas exitosas

## Cierre

Con este cambio, el balance ya no refleja sólo ventas; ahora también refleja egresos del día y entrega una referencia correcta para el cuadre de caja.
