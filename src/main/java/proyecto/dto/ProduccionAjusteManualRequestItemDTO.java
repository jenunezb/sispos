package proyecto.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProduccionAjusteManualRequestItemDTO(
        @NotNull Long productoId,
        @NotNull @Min(0) Integer stockNuevo
) {
}
