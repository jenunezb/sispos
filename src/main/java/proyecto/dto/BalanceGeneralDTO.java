package proyecto.dto;

public record BalanceGeneralDTO(
        Double totalVentas,
        Double costoProduccion,
        Double utilidadBruta,
        Double valorInventario,
        Integer stockTotal,
        Long cantidadVentas
) {
}
