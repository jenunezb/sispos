package proyecto.dto;

import proyecto.entidades.EstadoCaja;

import java.time.LocalDateTime;

public record CajaTurnoResponseDTO(
        Long id,
        Long sedeId,
        String sedeNombre,
        EstadoCaja estado,
        LocalDateTime fechaApertura,
        LocalDateTime fechaCierre,
        Double baseInicial,
        CajaResumenDTO resumen,
        String observacion,
        String observacionCierre,
        Integer administradorAperturaId,
        String administradorAperturaNombre,
        Integer administradorCierreId,
        String administradorCierreNombre,
        Integer vendedorAperturaId,
        String vendedorAperturaNombre,
        Integer vendedorCierreId,
        String vendedorCierreNombre
) {
}
