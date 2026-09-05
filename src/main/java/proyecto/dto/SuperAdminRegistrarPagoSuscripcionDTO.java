package proyecto.dto;

import java.time.LocalDate;

public record SuperAdminRegistrarPagoSuscripcionDTO(
        Long sedeId,
        String tipoPago,
        Double valor,
        LocalDate fechaPago,
        String medioPago,
        String observacion
) {
}
