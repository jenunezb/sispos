package proyecto.dto;

import java.time.LocalDate;
import java.util.List;

public record SuperAdminSuscripcionSedeDTO(
        Long suscripcionId,
        Boolean configurada,
        Long empresaNit,
        String empresaNombre,
        Long sedeId,
        String sedeUbicacion,
        String tipoCobro,
        Double precioMensual,
        Double precioAnual,
        LocalDate fechaInicioServicio,
        LocalDate fechaUltimoPago,
        LocalDate fechaProximoVencimiento,
        String estadoServicio,
        Boolean alDia,
        Long diasRestantes,
        Boolean activa,
        String observacion,
        List<SuperAdminPagoSuscripcionDTO> pagos
) {
}
