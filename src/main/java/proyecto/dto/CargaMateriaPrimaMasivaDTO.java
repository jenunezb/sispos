package proyecto.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CargaMateriaPrimaMasivaDTO(
        @NotNull Long sedeId,
        @NotEmpty List<@Valid CargaMateriaPrimaItemDTO> items
) {}
