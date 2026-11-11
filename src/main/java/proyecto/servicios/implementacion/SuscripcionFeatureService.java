package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import proyecto.entidades.PlanSuscripcionSede;
import proyecto.entidades.SuscripcionSede;
import proyecto.repositorios.SuscripcionSedeRepository;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SuscripcionFeatureService {

    private final SuscripcionSedeRepository suscripcionSedeRepository;

    public void validarGastosHabilitados(Long sedeId) {
        SuscripcionSede suscripcion = suscripcionSedeRepository.findBySedeId(sedeId)
                .orElseThrow(() -> new RuntimeException("La sede no tiene una suscripcion configurada para usar gastos"));

        if (!tieneGastosHabilitados(suscripcion)) {
            throw new RuntimeException("La sede debe tener un plan PREMIUM activo para usar el modulo de gastos");
        }
    }

    public boolean tieneGastosHabilitados(Long sedeId) {
        return suscripcionSedeRepository.findBySedeId(sedeId)
                .map(this::tieneGastosHabilitados)
                .orElse(false);
    }

    public void validarCajaHabilitada(Long sedeId) {
        if (!tieneCajaHabilitada(sedeId)) {
            throw new RuntimeException("La sede debe tener un plan PREMIUM activo para usar el modulo de caja");
        }
    }

    public boolean tieneCajaHabilitada(Long sedeId) {
        return tieneGastosHabilitados(sedeId);
    }

    public String obtenerPlan(Long sedeId) {
        return suscripcionSedeRepository.findBySedeId(sedeId)
                .map(suscripcion -> suscripcion.getPlan() != null ? suscripcion.getPlan().name() : PlanSuscripcionSede.BASICO.name())
                .orElse(PlanSuscripcionSede.BASICO.name());
    }

    public Set<Long> obtenerSedeIdsConGastosHabilitados(Long empresaNit) {
        return suscripcionSedeRepository.findBySedeEmpresaNit(empresaNit).stream()
                .filter(this::tieneGastosHabilitados)
                .map(suscripcion -> suscripcion.getSede().getId())
                .collect(Collectors.toSet());
    }

    public boolean tieneGastosHabilitados(SuscripcionSede suscripcion) {
        if (suscripcion == null || suscripcion.getSede() == null) {
            return false;
        }

        if (suscripcion.getPlan() != PlanSuscripcionSede.PREMIUM) {
            return false;
        }

        if (!Boolean.TRUE.equals(suscripcion.getActiva())) {
            return false;
        }

        return suscripcion.getFechaProximoVencimiento() != null
                && !suscripcion.getFechaProximoVencimiento().isBefore(LocalDate.now());
    }
}
