package proyecto.dto;

import java.util.List;

public record AjusteManualMasivoResponseDTO(
        Long sedeId,
        Integer actualizados,
        List<AjusteManualResultadoItemDTO> resultado
) {
}
