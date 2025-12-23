package proyecto.dto;

import java.util.List;

public record InventarioVendedorResponseDTO(
        Long sedeId,
        List<InventarioDTO> inventario
) {
}
