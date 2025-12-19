package proyecto.dto;

public record BalanceSedeDTO(
        Long sedeId,
        String sedeNombre,
        Double totalVentas,
        Double costoProduccion,
        Double utilidadBruta,
        Double valorInventario,
        Integer stockActual,
        Long cantidadVentas

) {
}
