package proyecto.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record ActualizarConsumoProductoDTO(
        @PositiveOrZero
        double mlConsumidos
) {
}
