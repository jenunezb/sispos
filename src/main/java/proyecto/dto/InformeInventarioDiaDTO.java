package proyecto.dto;

import java.time.LocalDate;
import java.util.List;

public record InformeInventarioDiaDTO(
        Long sedeId,
        LocalDate fecha,
        List<InventarioDelDia> inventarioDia,
        List<MateriaPrimaInventarioDTO> materiaPrimaDia,
        double totalVendido
) {}
