package proyecto.dto;

import jakarta.validation.constraints.NotNull;
import proyecto.entidades.EstadoComandaCocina;

public record ComandaCocinaEstadoDTO(
        @NotNull(message = "El estado es obligatorio")
        EstadoComandaCocina estado
) {}
