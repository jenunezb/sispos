package proyecto.dto;

public record InventarioAjustableItemDTO(
        Long id,
        TipoInventarioAjustable tipo,
        String nombre,
        Double stockActual
) {
}
