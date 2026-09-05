package proyecto.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CajaAperturaDTO(
        @NotNull(message = "La sede es obligatoria")
        Long sedeId,

        @NotNull(message = "La base inicial es obligatoria")
        @PositiveOrZero(message = "La base inicial no puede ser negativa")
        Double baseInicial,

        String observacion
) {
}
