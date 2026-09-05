package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import proyecto.dto.SedeActualizarDTO;
import proyecto.dto.SedeCrearDTO;
import proyecto.dto.SedeDTO;
import proyecto.entidades.Empresa;
import proyecto.entidades.Sede;
import proyecto.repositorios.EmpresaRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.servicios.interfaces.SedeServicio;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SedeServicioIpml implements SedeServicio {
    private final SedeRepository sedeRepository;
    private final EmpresaRepository empresaRepository;

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
        return new SedeDTO(
                sede.getId(),
                sede.getUbicacion()
        );
    }

    public SedeDTO obtenerPorId(Long id) {
        Sede sede = sedeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        return toDTO(sede);
    }

}
