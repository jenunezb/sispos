ALTER TABLE venta
    DROP CONSTRAINT IF EXISTS venta_modo_pago_check;

ALTER TABLE venta
    ADD CONSTRAINT venta_modo_pago_check
        CHECK (modo_pago IN ('EFECTIVO', 'TRANSFERENCIA', 'MIXTO'));
