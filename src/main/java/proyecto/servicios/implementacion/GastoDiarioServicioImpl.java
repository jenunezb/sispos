package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import proyecto.dto.GastoDiarioCrearDTO;
import proyecto.dto.GastoDiarioResponseDTO;
import proyecto.entidades.Administrador;
import proyecto.entidades.GastoDiario;
import proyecto.entidades.ModoPago;
import proyecto.entidades.Sede;
import proyecto.entidades.Vendedor;
import proyecto.repositorios.GastoDiarioRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.servicios.interfaces.GastoDiarioServicio;
import proyecto.utils.FechaColombiaUtils;

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
        validarModoPago(dto.modoPago());

        GastoDiario gasto = new GastoDiario();
        gasto.setSede(sede);
        gasto.setAdministrador(administrador);
        gasto.setDescripcion(dto.descripcion().trim());
        gasto.setValor(dto.valor());
        gasto.setModoPago(dto.modoPago());
        gasto.setFecha(FechaColombiaUtils.ahora());

        return mapToResponse(gastoDiarioRepository.save(gasto));
    }

    @Override
    public GastoDiarioResponseDTO crear(Vendedor vendedor, GastoDiarioCrearDTO dto) {
        Sede sede = sedeRepository.findById(dto.sedeId())
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));
        validarModoPago(dto.modoPago());

        GastoDiario gasto = new GastoDiario();
        gasto.setSede(sede);
        gasto.setVendedor(vendedor);
        gasto.setDescripcion(dto.descripcion().trim());
        gasto.setValor(dto.valor());
        gasto.setModoPago(dto.modoPago());
        gasto.setFecha(FechaColombiaUtils.ahora());

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
        String administradorNombre = nombreCompletoAdministrador(gasto.getAdministrador());
        String vendedorNombre = nombreCompletoVendedor(gasto.getVendedor());
        String registradoPorRol = gasto.getVendedor() != null ? "VENDEDOR" : "ADMINISTRADOR";

        return new GastoDiarioResponseDTO(
                gasto.getId(),
                gasto.getSede().getId(),
                gasto.getSede().getUbicacion(),
                gasto.getDescripcion(),
                gasto.getValor(),
                gasto.getModoPago(),
                gasto.getFecha(),
                gasto.getAdministrador() != null ? gasto.getAdministrador().getCodigo() : null,
                administradorNombre,
                gasto.getVendedor() != null ? gasto.getVendedor().getCodigo() : null,
                vendedorNombre,
                registradoPorRol
        );
    }

    private String nombreCompletoAdministrador(Administrador administrador) {
        if (administrador == null) {
            return null;
        }
        String nombre = administrador.getNombre() != null ? administrador.getNombre().trim() : "";
        String apellido = administrador.getApellido() != null ? administrador.getApellido().trim() : "";
        String nombreCompleto = (nombre + " " + apellido).trim();
        return nombreCompleto.isBlank() ? null : nombreCompleto;
    }

    private String nombreCompletoVendedor(Vendedor vendedor) {
        if (vendedor == null) {
            return null;
        }
        String nombre = vendedor.getNombre() != null ? vendedor.getNombre().trim() : "";
        return nombre.isBlank() ? null : nombre;
    }

    private void validarModoPago(ModoPago modoPago) {
        if (modoPago == ModoPago.MIXTO) {
            throw new RuntimeException("El gasto diario no admite modo de pago mixto");
        }
    }
}
