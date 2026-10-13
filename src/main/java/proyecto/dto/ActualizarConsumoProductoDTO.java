package proyecto.dto;

import jakarta.validation.constraints.Positive;

public record ActualizarConsumoProductoDTO(
        @Positive
        double mlConsumidos
) {
}
