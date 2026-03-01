package proyecto.dto;

public record VendedorDTO(
        Integer codigo,
        String nombre,
        String cedula,
        String correo,
        String telefono,
        Boolean estado
) {
}
