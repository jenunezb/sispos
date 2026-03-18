package proyecto.dto;

import java.time.LocalDate;

public record SuperAdminConfigurarSuscripcionDTO(
        Long sedeId,
        String tipoCobro,
        Double precioMensual,
        Double precioAnual,
        LocalDate fechaInicioServicio,
        String observacion,
        Boolean activa
) {
}
