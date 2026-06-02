package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import proyecto.dto.GastoDiarioCrearDTO;
import proyecto.dto.GastoDiarioResponseDTO;
import proyecto.entidades.Administrador;
import proyecto.entidades.GastoDiario;
import proyecto.entidades.Sede;
import proyecto.repositorios.GastoDiarioRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.servicios.interfaces.GastoDiarioServicio;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GastoDiarioServicioImpl implements GastoDiarioServicio {

    private final GastoDiarioRepository gastoDiarioRepository;
    private final SedeRepository sedeRepository;

    @Override
    public GastoDiarioResponseDTO crear(Administrador administrador, GastoDiarioCrearDTO dto) {
        Sede sede = sedeRepository.findById(dto.sedeId())
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        GastoDiario gasto = new GastoDiario();
        gasto.setSede(sede);
        gasto.setAdministrador(administrador);
        gasto.setDescripcion(dto.descripcion().trim());
        gasto.setValor(dto.valor());
        gasto.setModoPago(dto.modoPago());
        gasto.setFecha(LocalDateTime.now());

        return mapToResponse(gastoDiarioRepository.save(gasto));
    }

    @Override
    public List<GastoDiarioResponseDTO> listar(Long empresaNit, Long sedeId, LocalDateTime desde, LocalDateTime hasta) {
        return gastoDiarioRepository.listarPorEmpresaYSedeEntreFechas(empresaNit, sedeId, desde, hasta)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private GastoDiarioResponseDTO mapToResponse(GastoDiario gasto) {
        String nombre = gasto.getAdministrador().getNombre() != null
                ? gasto.getAdministrador().getNombre().trim()
                : "";
        String apellido = gasto.getAdministrador().getApellido() != null
                ? gasto.getAdministrador().getApellido().trim()
                : "";
        String administradorNombre = (nombre + " " + apellido).trim();

        return new GastoDiarioResponseDTO(
                gasto.getId(),
                gasto.getSede().getId(),
                gasto.getSede().getUbicacion(),
                gasto.getDescripcion(),
                gasto.getValor(),
                gasto.getModoPago(),
                gasto.getFecha(),
                gasto.getAdministrador().getCodigo(),
                administradorNombre.isBlank() ? null : administradorNombre
        );
    }
}
