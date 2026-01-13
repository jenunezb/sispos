package proyecto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CrearMateriaPrimaSedeDTO(

        @NotBlank
        String nombre,

        boolean activa,

        @NotNull
        Long sedeId,

        @PositiveOrZero
        double cantidadInicialMl,

        @PositiveOrZero
        double mlPorVaso

) {}

