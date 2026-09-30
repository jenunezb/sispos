package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import proyecto.entidades.EstadoSuscripcionSede;
import proyecto.entidades.Sede;
import proyecto.entidades.SuscripcionSede;
import proyecto.repositorios.SuscripcionSedeRepository;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SuscripcionSedeInicializacionService {

    public static final String OBSERVACION_PENDIENTE_ACTIVACION =
            "Pendiente de activacion por soporte tecnico";
    public static final String OBSERVACION_PERIODO_PRUEBA =
            "Periodo de prueba gratuito por registro de empresa";

    private final SuscripcionSedeRepository suscripcionSedeRepository;

    public SuscripcionSede crearPendienteActivacion(Sede sede) {
        SuscripcionSede suscripcion = new SuscripcionSede();
        suscripcion.setSede(sede);
        suscripcion.setActiva(true);
        suscripcion.setEstadoServicio(EstadoSuscripcionSede.VENCIDO);
        suscripcion.setFechaProximoVencimiento(LocalDate.now().minusDays(1));
        suscripcion.setObservacion(OBSERVACION_PENDIENTE_ACTIVACION);
        return suscripcionSedeRepository.save(suscripcion);
    }

    public SuscripcionSede crearPeriodoPrueba(Sede sede) {
        LocalDate hoy = LocalDate.now();
        SuscripcionSede suscripcion = new SuscripcionSede();
        suscripcion.setSede(sede);
        suscripcion.setActiva(true);
        suscripcion.setEstadoServicio(EstadoSuscripcionSede.ACTIVO);
        suscripcion.setFechaInicioServicio(hoy);
        suscripcion.setFechaProximoVencimiento(hoy.plusMonths(1));
        suscripcion.setObservacion(OBSERVACION_PERIODO_PRUEBA);
        return suscripcionSedeRepository.save(suscripcion);
    }
}
