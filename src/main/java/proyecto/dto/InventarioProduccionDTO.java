package proyecto.dto;

public record InventarioProduccionDTO(
        Long productoId,
        String productoNombre,
        Integer stockActual,
        Integer producidoAcumulado,
        Integer despachadoAcumulado
) {
}
