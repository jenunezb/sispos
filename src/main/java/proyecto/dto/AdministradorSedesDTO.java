package proyecto.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AdministradorSedesDTO(
        @NotEmpty(message = "Debe asignar al menos una sede")
        List<Long> sedeIds
) {
}
