package proyecto.dto;

import proyecto.entidades.EstadoComandaCocina;

import java.time.LocalDateTime;
import java.util.List;

public record ComandaCocinaResponseDTO(
        Long id,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion,
        String nombreMesa,
        String observaciones,
        EstadoComandaCocina estado,
        Integer totalItems,
        Long sedeId,
        String sedeNombre,
        String responsable,
        List<ComandaCocinaDetalleDTO> detalles
) {}
