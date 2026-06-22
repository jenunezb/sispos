DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'venta_modo_pago_check'
    ) THEN
        ALTER TABLE venta
            DROP CONSTRAINT venta_modo_pago_check;
    END IF;
END $$;

ALTER TABLE venta
    ADD CONSTRAINT venta_modo_pago_check
        CHECK (modo_pago IN ('EFECTIVO', 'TRANSFERENCIA', 'MIXTO'));
