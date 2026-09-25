package proyecto.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AjusteManualMasivoRequestDTO(
        @NotNull Long sedeId,
        @NotEmpty List<@Valid AjusteManualItemDTO> items
) {
}
