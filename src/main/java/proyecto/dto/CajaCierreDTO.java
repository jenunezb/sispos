package proyecto.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CajaCierreDTO(
        @NotNull(message = "El efectivo contado es obligatorio")
        @PositiveOrZero(message = "El efectivo contado no puede ser negativo")
        Double efectivoContado,

        String observacionCierre
) {
}
