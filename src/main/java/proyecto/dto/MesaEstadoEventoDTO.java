package proyecto.dto;

public record MesaEstadoEventoDTO(
        String tipo,
        Long sedeId,
        MesaEstadoDTO mesa
) {}
