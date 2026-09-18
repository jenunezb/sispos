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
        Double totalGastos,
        Double gastosEfectivo,
        Double gastosTransferencia,
        Double cajaEsperada,
        Double utilidadNeta,
        Double valorInventario,
        Integer stockActual,
        Long cantidadVentas

) {
}
