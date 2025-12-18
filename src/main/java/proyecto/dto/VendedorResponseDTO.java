package proyecto.dto;

public record VendedorResponseDTO(
        Long id,
        String nombre,
        String correo,
        String telefono
) {}