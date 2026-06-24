package proyecto.dto;

import java.time.LocalDate;

public record VentasPorDiaDTO(
        LocalDate fecha,
        String diaSemana,
        Double totalVentas,
        Long cantidadVentas
) {
}
