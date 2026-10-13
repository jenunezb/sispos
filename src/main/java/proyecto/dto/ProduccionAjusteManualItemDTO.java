package proyecto.dto;

public record ProduccionAjusteManualItemDTO(
        Long productoId,
        String productoNombre,
        Integer stockActual
) {
}
