\set ON_ERROR_STOP on

BEGIN;

CREATE TEMP TABLE src_materia_prima (
    codigo bigint,
    activa boolean,
    nombre text
);

CREATE TEMP TABLE src_materia_prima_sede (
    id bigint,
    activa boolean,
    cantidad_actual_ml double precision,
    ml_por_vaso double precision,
    materia_prima_id bigint,
    sede_id bigint
);

CREATE TEMP TABLE src_producto_materia_prima (
    id bigint,
    ml_consumidos double precision,
    materia_prima_id bigint,
    producto_id bigint
);

CREATE TEMP TABLE src_producto (
    codigo bigint,
    categoria text,
    descripcion text,
    estado boolean,
    nombre text,
    precio_produccion double precision,
    precio_venta double precision
);

\copy src_materia_prima FROM 'C:/temp/migracion-delicias-calle20/materia_prima.csv' WITH (FORMAT csv, HEADER true, ENCODING 'WIN1252')
\copy src_materia_prima_sede FROM 'C:/temp/migracion-delicias-calle20/materia_prima_sede.csv' WITH (FORMAT csv, HEADER true, ENCODING 'WIN1252')
\copy src_producto_materia_prima FROM 'C:/temp/migracion-delicias-calle20/producto_materia_prima.csv' WITH (FORMAT csv, HEADER true, ENCODING 'WIN1252')
\copy src_producto FROM 'C:/temp/migracion-delicias-calle20/producto.csv' WITH (FORMAT csv, HEADER true, ENCODING 'WIN1252')

CREATE TEMP TABLE src_mp_norm AS
SELECT
    codigo AS source_mp_id,
    activa,
    nombre AS source_nombre,
    regexp_replace(
        translate(lower(trim(COALESCE(nombre, ''))), 'áéíóúüñ', 'aeiouun'),
        '[^a-z0-9]+',
        '',
        'g'
    ) AS normalized_name
FROM src_materia_prima;

CREATE TEMP TABLE dst_mp_norm AS
SELECT
    MIN(codigo) AS target_mp_id,
    regexp_replace(
        translate(lower(trim(COALESCE(nombre, ''))), 'áéíóúüñ', 'aeiouun'),
        '[^a-z0-9]+',
        '',
        'g'
    ) AS normalized_name
FROM materia_prima
GROUP BY 2;

INSERT INTO materia_prima (nombre, activa)
SELECT s.source_nombre, s.activa
FROM src_mp_norm s
LEFT JOIN dst_mp_norm d
    ON d.normalized_name = s.normalized_name
WHERE d.target_mp_id IS NULL;

TRUNCATE TABLE dst_mp_norm;

INSERT INTO dst_mp_norm (target_mp_id, normalized_name)
SELECT
    MIN(codigo) AS target_mp_id,
    regexp_replace(
        translate(lower(trim(COALESCE(nombre, ''))), 'áéíóúüñ', 'aeiouun'),
        '[^a-z0-9]+',
        '',
        'g'
    ) AS normalized_name
FROM materia_prima
GROUP BY 2;

CREATE TEMP TABLE mp_map AS
SELECT
    s.source_mp_id,
    d.target_mp_id,
    s.source_nombre
FROM src_mp_norm s
JOIN dst_mp_norm d
    ON d.normalized_name = s.normalized_name;

CREATE TEMP TABLE src_producto_norm AS
SELECT
    codigo AS source_producto_id,
    regexp_replace(
        translate(lower(trim(COALESCE(nombre, ''))), 'áéíóúüñ', 'aeiouun'),
        '[^a-z0-9]+',
        '',
        'g'
    ) AS normalized_name
FROM src_producto;

CREATE TEMP TABLE dst_producto_norm AS
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
    s.source_producto_id,
    d.target_producto_id
FROM src_producto_norm s
JOIN dst_producto_norm d
    ON d.normalized_name = s.normalized_name;

DELETE FROM producto_materia_prima
WHERE producto_id IN (
    SELECT producto_id
    FROM inventario
    WHERE sede_id = 7
);

DELETE FROM materia_prima_sede
WHERE sede_id = 7;

INSERT INTO materia_prima_sede (
    activa,
    cantidad_actual_ml,
    ml_por_vaso,
    materia_prima_id,
    sede_id
)
SELECT
    s.activa,
    s.cantidad_actual_ml,
    s.ml_por_vaso,
    m.target_mp_id,
    7
FROM src_materia_prima_sede s
JOIN mp_map m
    ON m.source_mp_id = s.materia_prima_id
WHERE s.sede_id = 3
ORDER BY s.id;

INSERT INTO producto_materia_prima (
    ml_consumidos,
    materia_prima_id,
    producto_id
)
SELECT
    spmp.ml_consumidos,
    mm.target_mp_id,
    pm.target_producto_id
FROM src_producto_materia_prima spmp
JOIN mp_map mm
    ON mm.source_mp_id = spmp.materia_prima_id
JOIN producto_map pm
    ON pm.source_producto_id = spmp.producto_id
ORDER BY spmp.id;

COMMIT;

SELECT mp.nombre, mps.cantidad_actual_ml, mps.ml_por_vaso
FROM materia_prima_sede mps
JOIN materia_prima mp
    ON mp.codigo = mps.materia_prima_id
WHERE mps.sede_id = 7
ORDER BY mp.nombre;

SELECT COUNT(*) AS relaciones_producto_mp
FROM producto_materia_prima
WHERE producto_id IN (
    SELECT producto_id
    FROM inventario
    WHERE sede_id = 7
);
