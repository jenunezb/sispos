package proyecto.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ProduccionRegistroMultipleDTO(
        @NotEmpty(message = "Debe registrar al menos un producto")
        List<@Valid ProduccionRegistroItemDTO> items,

        String observacion
) {
}
