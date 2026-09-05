package proyecto.dto;

import java.util.List;

public record ProduccionAjusteManualResponseDTO(
        List<ProduccionAjusteManualItemDTO> items
) {
}
