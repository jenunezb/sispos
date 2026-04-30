\set ON_ERROR_STOP on

BEGIN;

CREATE TEMP TABLE src_producto (
    codigo bigint,
    categoria text,
    descripcion text,
    estado boolean,
    nombre text,
    precio_produccion double precision,
    precio_venta double precision
);

CREATE TEMP TABLE src_inventario (
    id bigint,
    entradas integer,
    perdidas integer,
    salidas integer,
    stock_actual integer,
    producto_id bigint,
    sede_id bigint
);

\copy src_producto FROM 'C:/temp/migracion-delicias-calle20/producto.csv' WITH (FORMAT csv, HEADER true, ENCODING 'WIN1252')
\copy src_inventario FROM 'C:/temp/migracion-delicias-calle20/inventario.csv' WITH (FORMAT csv, HEADER true, ENCODING 'WIN1252')

CREATE TEMP TABLE src_producto_normalizado AS
SELECT
    p.codigo AS source_producto_id,
    regexp_replace(
        translate(lower(trim(COALESCE(p.nombre, ''))), 'áéíóúüñ', 'aeiouun'),
        '[^a-z0-9]+',
        '',
        'g'
    ) AS normalized_name
FROM src_producto p;

CREATE TEMP TABLE dst_producto_normalizado AS
SELECT
    p.codigo AS target_producto_id,
    regexp_replace(
        translate(lower(trim(COALESCE(p.nombre, ''))), 'áéíóúüñ', 'aeiouun'),
        '[^a-z0-9]+',
        '',
        'g'
    ) AS normalized_name
FROM producto p
JOIN inventario i
    ON i.producto_id = p.codigo
WHERE i.sede_id = 7;

CREATE TEMP TABLE producto_map AS
SELECT
    sp.source_producto_id,
    dp.target_producto_id
FROM src_producto_normalizado sp
JOIN dst_producto_normalizado dp
    ON dp.normalized_name = sp.normalized_name;

CREATE TEMP TABLE inventario_stage AS
SELECT
    pm.target_producto_id,
    SUM(COALESCE(si.entradas, 0)) AS entradas,
    SUM(COALESCE(si.perdidas, 0)) AS perdidas,
    SUM(COALESCE(si.salidas, 0)) AS salidas,
    SUM(COALESCE(si.stock_actual, 0)) AS stock_actual
FROM src_inventario si
JOIN producto_map pm
    ON pm.source_producto_id = si.producto_id
WHERE si.sede_id = 3
GROUP BY pm.target_producto_id;

UPDATE inventario
SET
    entradas = 0,
    perdidas = 0,
    salidas = 0,
    stock_actual = 0
WHERE sede_id = 7;

UPDATE inventario i
SET
    entradas = s.entradas,
    perdidas = s.perdidas,
    salidas = s.salidas,
    stock_actual = s.stock_actual
FROM inventario_stage s
WHERE i.sede_id = 7
  AND i.producto_id = s.target_producto_id;

COMMIT;

SELECT
    i.producto_id,
    p.nombre,
    i.entradas,
    i.perdidas,
    i.salidas,
    i.stock_actual
FROM inventario i
JOIN producto p
    ON p.codigo = i.producto_id
WHERE i.sede_id = 7
ORDER BY i.producto_id;
