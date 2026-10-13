package proyecto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CargaMateriaPrimaItemDTO(
        @NotBlank String nombre,
        @NotBlank String unidadBase,
        @NotBlank String presentacion,
        @Positive double cantidadPresentaciones,
        @Positive double contenidoPresentacion,
        @Positive double precioPresentacion
) {}
