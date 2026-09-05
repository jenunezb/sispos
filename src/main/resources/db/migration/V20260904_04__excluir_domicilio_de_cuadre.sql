-- El domicilio se cobra al cliente, pero se entrega al domiciliario y no es ingreso de la empresa.
WITH ventas_domicilio AS (
    SELECT id,
           COALESCE(costo_domicilio, 0) AS costo,
           COALESCE(monto_efectivo, CASE WHEN modo_pago = 'EFECTIVO' THEN total ELSE 0 END) AS efectivo_original,
           COALESCE(monto_transferencia, CASE WHEN modo_pago = 'TRANSFERENCIA' THEN total ELSE 0 END) AS transferencia_original
    FROM venta
    WHERE es_domicilio = TRUE
      AND COALESCE(costo_domicilio, 0) > 0
)
UPDATE venta v
SET total = GREATEST(0, v.total - d.costo),
    monto_efectivo = GREATEST(0, d.efectivo_original - d.costo),
    monto_transferencia = GREATEST(
        0,
        d.transferencia_original - GREATEST(0, d.costo - d.efectivo_original)
    )
FROM ventas_domicilio d
WHERE v.id = d.id;
