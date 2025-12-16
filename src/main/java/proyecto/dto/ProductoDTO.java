package proyecto.dto;

public record ProductoDTO(Long codigo,
                          String nombre,
                          String descripcion,
                          Double precioProduccion,
                          Double precioVenta,
                          Integer cantidad,
                          String categoria,
                          Boolean estado) {
}
