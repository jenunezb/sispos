package proyecto.dto;

public record PrecioClienteDTO(
        Long id,
        Long clienteId,
        Long productoCodigo,
        String productoNombre,
        Double precioVenta,
        Boolean activo
) {
}
