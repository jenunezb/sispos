package proyecto.dto;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record BalanceSedeVendedor(
        Long sedeId,
        String sedeNombre,
        Double totalVentas,
        Double ventasEfectivo,
        Double ventasTransferencia,
        Long cantidadVentas,
        String plan,
        Boolean gastosHabilitados,
        Boolean cajaHabilitada,
        Double totalGastos,
        Double gastosEfectivo,
        Double cajaEsperada
) {
}
