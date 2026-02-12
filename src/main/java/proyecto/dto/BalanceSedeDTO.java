package proyecto.dto;

import lombok.Builder;

@Builder
public record BalanceSedeDTO(
        Long sedeId,
        String sedeNombre,
        Double totalVentas,
        Double efectivo,
        Double trasferencia,
        Double costoProduccion,
        Double utilidadBruta,
        Double valorInventario,
        Integer stockActual,
        Long cantidadVentas

) {
}
