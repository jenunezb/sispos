package proyecto.dto;

public record InventarioDTO(
        Long id,

        Long productoId,
        String productoNombre,

        Integer stockActual,
        Integer entradas,
        Integer salidas,
        Integer perdidas

) {
}
