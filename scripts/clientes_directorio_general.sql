-- Opcional: anticipa la misma migracion incluida en schema.sql al arrancar.
-- PostgreSQL. No duplica clientes ni altera los IDs usados por Produccion.
BEGIN;
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS correo VARCHAR(254);
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS direccion VARCHAR(255);
-- venta.cliente_id y su relacion ya existen en el esquema actual.
COMMIT;
