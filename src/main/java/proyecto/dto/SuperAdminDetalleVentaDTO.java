package proyecto.dto;

public record SuperAdminDetalleVentaDTO(
        Long detalleId,
        Long productoId,
        String productoNombre,
        String nombreLibre,
        Integer cantidad,
        Double precioUnitario,
        Double subtotal
) {
}
