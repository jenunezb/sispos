package proyecto.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ComandaCocinaCrearDTO(
        @NotBlank(message = "El correo es obligatorio")
        String correo,

        @NotNull(message = "La sede es obligatoria")
        Long sedeId,

        @NotBlank(message = "La mesa es obligatoria")
        String nombreMesa,

        String observaciones,

        @NotEmpty(message = "La comanda debe contener al menos un item")
        List<@Valid ComandaCocinaDetalleDTO> detalles
) {}
