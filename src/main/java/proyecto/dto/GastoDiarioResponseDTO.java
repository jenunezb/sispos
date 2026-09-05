package proyecto.dto;

import proyecto.entidades.ModoPago;

import java.time.LocalDateTime;

public record GastoDiarioResponseDTO(
        Long id,
        Long sedeId,
        String sedeNombre,
        String descripcion,
        Double valor,
        ModoPago modoPago,
        LocalDateTime fecha,
        Integer administradorId,
        String administradorNombre,
        Integer vendedorId,
        String vendedorNombre,
        String registradoPorRol
) {
}
