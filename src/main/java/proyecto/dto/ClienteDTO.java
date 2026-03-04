package proyecto.dto;

public record ClienteDTO(
        Long id,
        String nombre,
        String telefono,
        String documento,
        Boolean activo
) {
}
