package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import proyecto.dto.SedeActualizarDTO;
import proyecto.dto.SedeCrearDTO;
import proyecto.dto.SedeDTO;
import proyecto.entidades.Administrador;
import proyecto.entidades.Sede;
import proyecto.repositorios.SedeRepository;
import proyecto.servicios.interfaces.SedeServicio;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SedeServicioIpml implements SedeServicio {
    private final SedeRepository sedeRepository;

    public SedeDTO crear(SedeCrearDTO dto) {

        if (sedeRepository.existsByNombreIgnoreCase(dto.nombre())) {
            throw new IllegalArgumentException("Ya existe una sede con ese nombre");
        }

        Sede sede = new Sede();
        sede.setNombre(dto.nombre());
        sede.setUbicacion(dto.ubicacion());

        return toDTO(sedeRepository.save(sede));
    }

    public List<SedeDTO> listar() {
        return sedeRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public SedeDTO actualizar(SedeActualizarDTO dto) {

        Sede sede = sedeRepository.findById(dto.id())
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        sede.setNombre(dto.nombre());
        sede.setUbicacion(dto.ubicacion());

        return toDTO(sedeRepository.save(sede));
    }

    private SedeDTO toDTO(Sede sede) {
        return new SedeDTO(
                sede.getId(),
                sede.getNombre(),
                sede.getUbicacion()
        );
    }
}
