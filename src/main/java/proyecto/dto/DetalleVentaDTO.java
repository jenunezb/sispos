package proyecto.dto;

public record DetalleVentaDTO(
        Long productoId,
        String nombreLibre,     // ej: "Domicilio"
        Double precioUnitario,  // precio digitado
        Integer cantidad
) {
}