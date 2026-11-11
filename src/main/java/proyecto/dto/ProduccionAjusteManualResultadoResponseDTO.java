package proyecto.dto;

import java.util.List;

public record ProduccionAjusteManualResultadoResponseDTO(
        Integer actualizados,
        List<ProduccionAjusteManualResultadoItemDTO> resultado
) {
}
