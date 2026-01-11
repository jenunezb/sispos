package proyecto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record MateriaPrimaRequestDTO(
        String nombre,
        double cantidadInicialMl,
        double mlPorVaso,
        boolean activa
) {}

