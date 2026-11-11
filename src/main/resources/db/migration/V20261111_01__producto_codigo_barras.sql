-- PostgreSQL: aplicar antes de arrancar el backend (ddl-auto=validate; no hay Flyway automático).
ALTER TABLE producto ADD COLUMN IF NOT EXISTS codigo_barras VARCHAR(100);
CREATE UNIQUE INDEX IF NOT EXISTS uk_producto_empresa_codigo_barras
    ON producto (empresa_id, codigo_barras);
