package proyecto.dto;

import java.util.List;

public record InventarioAjustableResponseDTO(
        Long sedeId,
        List<InventarioAjustableItemDTO> items
) {
}
