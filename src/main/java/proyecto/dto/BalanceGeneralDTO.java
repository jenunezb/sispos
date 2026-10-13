package proyecto.dto;

public record BalanceGeneralDTO(
        Double totalVentas,
        Double costoProduccion,
        Double utilidadBruta,
        Double totalGastos,
        Double gastosEfectivo,
        Double gastosTransferencia,
        Double cajaEsperada,
        Double utilidadNeta,
        Double valorInventario,
        Integer stockTotal,
        Long cantidadVentas,
        Double ventasEfectivo,
        Double ventasTransferencia
) {
}
