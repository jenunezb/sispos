package proyecto.dto;

public record DetalleVentaResponseDTO(
        Long productoId,
        String productoNombre,
        Integer cantidad,
        Double precioUnitario,
        Double subtotal,
        String nombreLibre,
        java.util.List<DetalleVentaComplementoDTO> complementos
) {
    public DetalleVentaResponseDTO(Long productoId, String productoNombre, Integer cantidad,
                                   Double precioUnitario, Double subtotal, String nombreLibre) {
        this(productoId, productoNombre, cantidad, precioUnitario, subtotal, nombreLibre, java.util.List.of());
    }
}
