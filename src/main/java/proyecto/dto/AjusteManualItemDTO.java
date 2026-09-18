package proyecto.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AjusteManualItemDTO(
        @NotNull Long id,
        @NotNull TipoInventarioAjustable tipo,
        @NotNull @Min(0) Double stockNuevo
) {
}
