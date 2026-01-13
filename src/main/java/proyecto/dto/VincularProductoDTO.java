package proyecto.dto;

/**
 * DTO para vincular un producto a una materia prima en una sede
 */
public record VincularProductoDTO(
        Long materiaPrimaSedeId,
        Long productoId,
        double mlConsumidos
) {}
