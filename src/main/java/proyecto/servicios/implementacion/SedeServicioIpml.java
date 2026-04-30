package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import proyecto.dto.SedeActualizarDTO;
import proyecto.dto.SedeCrearDTO;
import proyecto.dto.SedeDTO;
import proyecto.entidades.Empresa;
import proyecto.entidades.Sede;
import proyecto.entidades.SuscripcionSede;
import proyecto.repositorios.EmpresaRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.repositorios.SuscripcionSedeRepository;
import proyecto.servicios.interfaces.SedeServicio;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SedeServicioIpml implements SedeServicio {
    private final SedeRepository sedeRepository;
    private final EmpresaRepository empresaRepository;
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

        return toDTO(sedeRepository.save(sede));
    }

    public List<SedeDTO> listar() {
        return listar(sedeRepository.findAll());
    }

    @Override
    public List<SedeDTO> listar(List<Sede> sedes) {
        Map<Long, SuscripcionSede> suscripcionesPorSede = obtenerSuscripcionesPorSede(sedes);
        return sedes.stream()
                .map(sede -> toDTO(sede, suscripcionesPorSede.get(sede.getId())))
                .toList();
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
        SuscripcionSede suscripcion = sede.getId() == null
                ? null
                : suscripcionSedeRepository.findBySedeId(sede.getId()).orElse(null);
        return toDTO(sede, suscripcion);
    }

    private SedeDTO toDTO(Sede sede, SuscripcionSede suscripcion) {
        return new SedeDTO(
                sede.getId(),
                sede.getUbicacion(),
                suscripcion != null ? suscripcion.getActiva() : false,
                suscripcion != null ? suscripcion.getFechaProximoVencimiento() : null
        );
    }

    private Map<Long, SuscripcionSede> obtenerSuscripcionesPorSede(List<Sede> sedes) {
        List<Long> sedeIds = sedes.stream()
                .map(Sede::getId)
                .filter(id -> id != null)
                .toList();

        if (sedeIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, SuscripcionSede> suscripcionesPorSede = new HashMap<>();
        suscripcionSedeRepository.findBySedeIdIn(sedeIds).forEach(suscripcion -> {
            if (suscripcion.getSede() != null && suscripcion.getSede().getId() != null) {
                suscripcionesPorSede.put(suscripcion.getSede().getId(), suscripcion);
            }
        });
        return suscripcionesPorSede;
    }

    public SedeDTO obtenerPorId(Long id) {
        Sede sede = sedeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        return toDTO(sede);
    }

}
