package proyecto.dto;

public record DetalleVentaResponseDTO(
        Long productoId,
        String productoNombre,
        Integer cantidad,
        Double precioUnitario,
        Double subtotal,
        String nombreLibre
) {}