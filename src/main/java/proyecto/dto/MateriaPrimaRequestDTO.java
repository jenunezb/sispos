package proyecto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import proyecto.entidades.Sede;

public record MateriaPrimaRequestDTO(
        String nombre,
        double cantidadInicialMl,
        double mlPorVaso,
        boolean activa,
        Long CodigoSede
) {}

