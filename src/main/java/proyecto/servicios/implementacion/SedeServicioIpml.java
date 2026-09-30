package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import proyecto.dto.SedeActualizarDTO;
import proyecto.dto.SedeCrearDTO;
import proyecto.dto.SedeDTO;
import proyecto.entidades.Empresa;
import proyecto.entidades.EstadoSuscripcionSede;
import proyecto.entidades.Sede;
import proyecto.entidades.SuscripcionSede;
import proyecto.repositorios.EmpresaRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.repositorios.SuscripcionSedeRepository;
import proyecto.servicios.interfaces.SedeServicio;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SedeServicioIpml implements SedeServicio {
    private final SedeRepository sedeRepository;
    private final EmpresaRepository empresaRepository;
    private final SuscripcionSedeInicializacionService suscripcionSedeInicializacionService;
    private final SuscripcionSedeRepository suscripcionSedeRepository;

    public SedeDTO crear(SedeCrearDTO dto, Long empresaNit) {

        if (sedeRepository.existsByUbicacionIgnoreCase(dto.nombre())) {
            throw new IllegalArgumentException("Ya existe una sede con ese nombre");
        }

        Empresa empresa = empresaRepository.findById(empresaNit)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        Sede sede = new Sede();
        sede.setUbicacion(dto.ubicacion());
        sede.setEmpresa(empresa);

        Sede sedeGuardada = sedeRepository.save(sede);
        suscripcionSedeInicializacionService.crearPendienteActivacion(sedeGuardada);
        return toDTO(sedeGuardada);
    }

    public List<SedeDTO> listar() {
        return listar(sedeRepository.findAll());
    }

    @Override
    public List<SedeDTO> listar(List<Sede> sedes) {
        return sedes.stream().map(this::toDTO).toList();
    }

    public List<SedeDTO> listarPorEmpresa(Long empresaNit) {
        return listar(sedeRepository.findByEmpresaNit(empresaNit));
    }

    public SedeDTO actualizar(SedeActualizarDTO dto) {

        Sede sede = sedeRepository.findById(dto.id())
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        sede.setUbicacion(dto.ubicacion());

        return toDTO(sedeRepository.save(sede));
    }

    private SedeDTO toDTO(Sede sede) {
        SuscripcionSede suscripcion = suscripcionSedeRepository.findBySedeId(sede.getId()).orElse(null);
        String estado = calcularEstadoSuscripcion(suscripcion);
        return new SedeDTO(
                sede.getId(),
                sede.getUbicacion(),
                estado,
                suscripcion != null ? suscripcion.getFechaProximoVencimiento() : null,
                construirMensajeSuscripcion(suscripcion, estado)
        );
    }

    private String construirMensajeSuscripcion(SuscripcionSede suscripcion, String estado) {
        if (suscripcion == null) {
            return null;
        }
        if (SuscripcionSedeInicializacionService.OBSERVACION_PENDIENTE_ACTIVACION
                .equals(suscripcion.getObservacion())) {
            return "La sede esta pendiente de activacion. Comunicate con soporte tecnico para habilitarla.";
        }
        if (SuscripcionSedeInicializacionService.OBSERVACION_PERIODO_PRUEBA
                .equals(suscripcion.getObservacion())
                && (EstadoSuscripcionSede.ACTIVO.name().equals(estado)
                || EstadoSuscripcionSede.POR_VENCER.name().equals(estado))) {
            return "Tu periodo de prueba gratuito esta activo hasta el "
                    + suscripcion.getFechaProximoVencimiento() + ".";
        }
        if (EstadoSuscripcionSede.VENCIDO.name().equals(estado)
                || EstadoSuscripcionSede.SUSPENDIDO.name().equals(estado)) {
            String fecha = suscripcion.getFechaProximoVencimiento() != null
                    ? suscripcion.getFechaProximoVencimiento().toString()
                    : "fecha no disponible";
            return "Tu suscripcion esta vencida desde el " + fecha
                    + ". Realiza el pago por llave al numero 3026367474 y, si ya pagaste, envia el comprobante para activar nuevamente el servicio.";
        }
        if (EstadoSuscripcionSede.POR_VENCER.name().equals(estado)) {
            return "Tu suscripcion esta por vencer.";
        }
        return null;
    }

    private String calcularEstadoSuscripcion(SuscripcionSede suscripcion) {
        if (suscripcion == null) {
            return "SIN_CONFIGURAR";
        }
        if (!Boolean.TRUE.equals(suscripcion.getActiva())) {
            return EstadoSuscripcionSede.SUSPENDIDO.name();
        }

        LocalDate vencimiento = suscripcion.getFechaProximoVencimiento();
        if (vencimiento == null || vencimiento.isBefore(LocalDate.now())) {
            return EstadoSuscripcionSede.VENCIDO.name();
        }
        if (!vencimiento.isAfter(LocalDate.now().plusDays(5))) {
            return EstadoSuscripcionSede.POR_VENCER.name();
        }
        return EstadoSuscripcionSede.ACTIVO.name();
    }

    public SedeDTO obtenerPorId(Long id) {
        Sede sede = sedeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        return toDTO(sede);
    }

}
