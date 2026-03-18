package proyecto.dto;

import java.time.LocalDate;

public record SuperAdminPagoSuscripcionDTO(
        Long pagoId,
        Long suscripcionId,
        Long empresaNit,
        String empresaNombre,
        Long sedeId,
        String sedeUbicacion,
        String tipoPago,
        Double valor,
        LocalDate fechaPago,
        LocalDate periodoDesde,
        LocalDate periodoHasta,
        String medioPago,
        String observacion,
        String registradoPor
) {
}
