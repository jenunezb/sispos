package proyecto.dto;

public record DetalleVentaDTO(
        Long productoId,
        String nombreLibre,     // ej: "Domicilio"
        Double precioUnitario,  // precio digitado
        Integer cantidad,
        java.util.List<ComplementoSeleccionDTO> complementos
) {
    public DetalleVentaDTO(Long productoId, String nombreLibre, Double precioUnitario, Integer cantidad) {
        this(productoId, nombreLibre, precioUnitario, cantidad, java.util.List.of());
    }
}
