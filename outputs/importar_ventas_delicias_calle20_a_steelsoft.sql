\set ON_ERROR_STOP on

BEGIN;

CREATE TEMP TABLE src_sede (
    id bigint,
    nombre text,
    ubicacion text,
    admin_id text
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

CREATE TEMP TABLE src_venta (
    id bigint,
    fecha timestamp,
    total double precision,
    sede_id bigint,
    vendedor_id integer,
    modo_pago text,
    anulado boolean
);

CREATE TEMP TABLE src_detalle (
    id bigint,
    cantidad integer,
    precio_unitario double precision,
    subtotal double precision,
    producto_id bigint,
    venta_id bigint,
    nombre_libre text
);

\copy src_sede FROM 'C:/temp/migracion-delicias-calle20/sede.csv' WITH (FORMAT csv, HEADER true, ENCODING 'WIN1252')
\copy src_producto FROM 'C:/temp/migracion-delicias-calle20/producto.csv' WITH (FORMAT csv, HEADER true, ENCODING 'WIN1252')
\copy src_venta FROM 'C:/temp/migracion-delicias-calle20/venta.csv' WITH (FORMAT csv, HEADER true, ENCODING 'WIN1252')
\copy src_detalle FROM 'C:/temp/migracion-delicias-calle20/detalle_venta.csv' WITH (FORMAT csv, HEADER true, ENCODING 'WIN1252')

CREATE TEMP TABLE venta_stage AS
SELECT
    v.id AS source_venta_id,
    v.fecha,
    v.total,
    7::bigint AS target_sede_id,
    v.modo_pago,
    COALESCE(v.anulado, false) AS anulado
FROM src_venta v
WHERE v.sede_id = 3;

CREATE TEMP TABLE producto_stage AS
SELECT
    p.codigo AS source_producto_id,
    regexp_replace(
        translate(lower(trim(COALESCE(p.nombre, ''))), 'áéíóúüñ', 'aeiouun'),
        '[^a-z0-9]+',
        '',
        'g'
    ) AS normalized_name
FROM src_producto p;

CREATE TEMP TABLE producto_map AS
SELECT
    ps.source_producto_id,
    dp.codigo AS target_producto_id
FROM producto_stage ps
JOIN producto dp
    ON regexp_replace(
           translate(lower(trim(COALESCE(dp.nombre, ''))), 'áéíóúüñ', 'aeiouun'),
           '[^a-z0-9]+',
           '',
           'g'
       ) = ps.normalized_name
JOIN inventario i
    ON i.producto_id = dp.codigo
WHERE i.sede_id = 7;

CREATE TEMP TABLE detalle_stage AS
SELECT
    d.id AS source_detalle_id,
    d.venta_id AS source_venta_id,
    d.cantidad,
    d.precio_unitario,
    d.subtotal,
    d.producto_id AS source_producto_id,
    d.nombre_libre,
    pm.target_producto_id
FROM src_detalle d
JOIN venta_stage vs
    ON vs.source_venta_id = d.venta_id
LEFT JOIN producto_map pm
    ON pm.source_producto_id = d.producto_id;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM detalle_stage
        WHERE source_producto_id IS NOT NULL
          AND target_producto_id IS NULL
    ) THEN
        RAISE EXCEPTION 'Existen detalles con producto sin mapeo hacia Steelsoft sede 7';
    END IF;
END $$;

CREATE TEMP TABLE ordered_sales AS
SELECT
    vs.*,
    COALESCE(mx.max_consecutivo, 0) +
    ROW_NUMBER() OVER (
        PARTITION BY vs.target_sede_id
        ORDER BY vs.fecha, vs.source_venta_id
    ) AS numero_consecutivo,
    ROW_NUMBER() OVER (
        ORDER BY vs.fecha, vs.source_venta_id
    ) AS rn
FROM venta_stage vs
LEFT JOIN (
    SELECT sede_id, MAX(numero_consecutivo) AS max_consecutivo
    FROM venta
    WHERE sede_id = 7
    GROUP BY sede_id
) mx
    ON mx.sede_id = vs.target_sede_id;

CREATE TEMP TABLE inserted_sales AS
WITH inserted AS (
    INSERT INTO venta (
        fecha,
        total,
        sede_id,
        vendedor_id,
        modo_pago,
        anulado,
        administrador_id,
        cliente_id,
        numero_consecutivo
    )
    SELECT
        fecha,
        total,
        target_sede_id,
        NULL,
        modo_pago,
        anulado,
        NULL,
        NULL,
        numero_consecutivo
    FROM ordered_sales
    ORDER BY rn
    RETURNING id
)
SELECT
    id AS target_venta_id,
    ROW_NUMBER() OVER (ORDER BY id) AS rn
FROM inserted;

CREATE TEMP TABLE venta_map AS
SELECT
    os.source_venta_id,
    ins.target_venta_id
FROM ordered_sales os
JOIN inserted_sales ins
    ON ins.rn = os.rn;

INSERT INTO detalle_venta (
    cantidad,
    precio_unitario,
    subtotal,
    producto_id,
    venta_id,
    nombre_libre
)
SELECT
    ds.cantidad,
    ds.precio_unitario,
    ds.subtotal,
    ds.target_producto_id,
    vm.target_venta_id,
    ds.nombre_libre
FROM detalle_stage ds
JOIN venta_map vm
    ON vm.source_venta_id = ds.source_venta_id
ORDER BY ds.source_detalle_id;

COMMIT;

SELECT COUNT(*) AS ventas_importadas FROM venta_map;
SELECT COUNT(*) AS detalles_importados FROM detalle_stage;
