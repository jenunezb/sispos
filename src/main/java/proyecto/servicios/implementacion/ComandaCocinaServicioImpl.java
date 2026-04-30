package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import proyecto.dto.ComandaCocinaCrearDTO;
import proyecto.dto.ComandaCocinaDetalleDTO;
import proyecto.dto.ComandaCocinaResponseDTO;
import proyecto.entidades.*;
import proyecto.repositorios.AdministradorRepository;
import proyecto.repositorios.ComandaCocinaRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.repositorios.VendedorRepository;
import proyecto.servicios.interfaces.ComandaCocinaServicio;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ComandaCocinaServicioImpl implements ComandaCocinaServicio {

    private static final List<EstadoComandaCocina> ESTADOS_ACTIVOS = List.of(
            EstadoComandaCocina.PENDIENTE,
            EstadoComandaCocina.EN_PREPARACION,
            EstadoComandaCocina.LISTA
    );

    private final ComandaCocinaRepository comandaCocinaRepository;
    private final SedeRepository sedeRepository;
    private final VendedorRepository vendedorRepository;
    private final AdministradorRepository administradorRepository;

    @Override
    @Transactional
    public ComandaCocinaResponseDTO crearComanda(ComandaCocinaCrearDTO dto) {
        if (dto.detalles() == null || dto.detalles().isEmpty()) {
            throw new RuntimeException("La comanda debe contener al menos un item");
        }

        UsuarioContexto contexto = resolverUsuario(dto.correo());
        Empresa empresa = contexto.obtenerEmpresa();

        if (!Boolean.TRUE.equals(empresa.getImpresionCocinaHabilitada())) {
            throw new RuntimeException("La impresion de cocina no esta habilitada para esta empresa");
        }

        Sede sede = sedeRepository.findById(dto.sedeId())
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        if (sede.getEmpresa() == null || !empresa.getNit().equals(sede.getEmpresa().getNit())) {
            throw new RuntimeException("La sede no pertenece a la empresa del usuario");
        }

        LocalDateTime ahora = ZonedDateTime.now(ZoneId.of("America/Bogota")).toLocalDateTime();

        ComandaCocina comanda = new ComandaCocina();
        comanda.setFechaCreacion(ahora);
        comanda.setFechaActualizacion(ahora);
        comanda.setNombreMesa(dto.nombreMesa().trim());
        comanda.setObservaciones(limpiarTexto(dto.observaciones()));
        comanda.setEstado(EstadoComandaCocina.PENDIENTE);
        comanda.setSede(sede);
        comanda.setVendedor(contexto.vendedor());
        comanda.setAdministrador(contexto.administrador());
        comanda.setTotalItems(dto.detalles().stream().mapToInt(ComandaCocinaDetalleDTO::cantidad).sum());

        List<ComandaCocinaDetalle> detalles = new ArrayList<>(dto.detalles().stream()
                .map(item -> {
                    ComandaCocinaDetalle detalle = new ComandaCocinaDetalle();
                    detalle.setComanda(comanda);
                    detalle.setProductoNombre(item.productoNombre().trim());
                    detalle.setCantidad(item.cantidad());
                    return detalle;
                })
                .toList());

        comanda.setDetalles(detalles);

        return mapToResponse(comandaCocinaRepository.save(comanda));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComandaCocinaResponseDTO> listarComandasActivas(String correo) {
        Empresa empresa = resolverUsuario(correo).obtenerEmpresa();

        return comandaCocinaRepository
                .findDetalleByEmpresaNitAndEstadoInOrderByFechaCreacionAsc(empresa.getNit(), ESTADOS_ACTIVOS)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public ComandaCocinaResponseDTO actualizarEstado(String correo, Long comandaId, EstadoComandaCocina estado) {
        Empresa empresa = resolverUsuario(correo).obtenerEmpresa();

        ComandaCocina comanda = comandaCocinaRepository.findDetalleByEmpresaNitAndId(empresa.getNit(), comandaId)
                .orElseThrow(() -> new RuntimeException("Comanda no encontrada"));

        comanda.setEstado(estado);
        comanda.setFechaActualizacion(ZonedDateTime.now(ZoneId.of("America/Bogota")).toLocalDateTime());

        return mapToResponse(comandaCocinaRepository.save(comanda));
    }

    private ComandaCocinaResponseDTO mapToResponse(ComandaCocina comanda) {
        String responsable = comanda.getVendedor() != null
                ? comanda.getVendedor().getNombre()
                : comanda.getAdministrador() != null ? comanda.getAdministrador().getNombre() : "Sin responsable";

        return new ComandaCocinaResponseDTO(
                comanda.getId(),
                comanda.getFechaCreacion(),
                comanda.getFechaActualizacion(),
                comanda.getNombreMesa(),
                comanda.getObservaciones(),
                comanda.getEstado(),
                comanda.getTotalItems(),
                comanda.getSede() != null ? comanda.getSede().getId() : null,
                comanda.getSede() != null ? comanda.getSede().getUbicacion() : null,
                responsable,
                comanda.getDetalles().stream()
                        .map(detalle -> new ComandaCocinaDetalleDTO(
                                detalle.getProductoNombre(),
                                detalle.getCantidad()
                        ))
                        .toList()
        );
    }

    private UsuarioContexto resolverUsuario(String correo) {
        String correoNormalizado = correo == null ? "" : correo.trim();
        Optional<Administrador> adminOpt = administradorRepository.findByCorreoIgnoreCase(correoNormalizado);
        if (adminOpt.isPresent()) {
            return new UsuarioContexto(adminOpt.get(), null);
        }

        Optional<Vendedor> vendedorOpt = vendedorRepository.findByCorreoIgnoreCase(correoNormalizado);
        if (vendedorOpt.isPresent()) {
            return new UsuarioContexto(null, vendedorOpt.get());
        }

        throw new RuntimeException("Usuario no autorizado");
    }

    private String limpiarTexto(String texto) {
        if (texto == null) {
            return null;
        }

        String limpio = texto.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private record UsuarioContexto(Administrador administrador, Vendedor vendedor) {

        private Empresa obtenerEmpresa() {
            if (administrador != null) {
                if (administrador.getEmpresa() == null) {
                    throw new RuntimeException("El administrador no tiene empresa asociada");
                }
                return administrador.getEmpresa();
            }

            if (vendedor != null) {
                if (vendedor.getEmpresa() != null) {
                    return vendedor.getEmpresa();
                }
                if (vendedor.getSede() != null && vendedor.getSede().getEmpresa() != null) {
                    return vendedor.getSede().getEmpresa();
                }
            }

            throw new RuntimeException("El usuario no tiene empresa asociada");
        }
    }
}
