package proyecto.dto;

public record ResumenProductoProduccionDTO(
        Long productoId,
        String productoNombre,
        Integer stockInicial,
        Integer producido,
        Integer despachado,
        Integer stockFinal
) {
}
