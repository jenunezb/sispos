package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import proyecto.dto.InventarioDTO;
import proyecto.dto.MesaEstadoDTO;
import proyecto.dto.MesaEstadoItemDTO;
import proyecto.entidades.*;
import proyecto.eventos.MesaEstadoActualizadoEvento;
import proyecto.excepciones.MesaVersionConflictException;
import proyecto.repositorios.AdministradorRepository;
import proyecto.repositorios.MesaEstadoRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.repositorios.VendedorRepository;
import proyecto.servicios.interfaces.MesaEstadoServicio;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MesaEstadoServicioImpl implements MesaEstadoServicio {

    private final MesaEstadoRepository mesaEstadoRepository;
    private final SedeRepository sedeRepository;
    private final AdministradorRepository administradorRepository;
    private final VendedorRepository vendedorRepository;
    private final AdministradorAccesoService administradorAccesoService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<MesaEstadoDTO> listarPorSede(String correo, String rol, Long sedeId) {
        validarAcceso(correo, rol, sedeId);
        return mesaEstadoRepository.findDetalleBySedeId(sedeId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional
    public MesaEstadoDTO guardarMesa(String correo, String rol, Long sedeId, Long mesaId, MesaEstadoDTO dto) {
        validarAcceso(correo, rol, sedeId);

        if (!mesaId.equals(dto.id())) {
            throw new RuntimeException("El id de la mesa no coincide con la ruta");
        }

        Sede sede = sedeRepository.findById(sedeId)
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        MesaEstado mesaEstado = mesaEstadoRepository.findDetalleBySedeIdAndMesaReferenciaId(sedeId, mesaId)
                .orElseGet(MesaEstado::new);

        if (mesaEstado.getId() != null && dto.version() != null && !dto.version().equals(mesaEstado.getVersion())) {
            throw new MesaVersionConflictException();
        }

        mesaEstado.setSede(sede);
        mesaEstado.setMesaReferenciaId(mesaId);
        mesaEstado.setNumero(dto.numero() != null ? dto.numero() : 0);
        mesaEstado.setNombre(limpiar(dto.nombre()));
        mesaEstado.setEstado(normalizarEstado(dto.estado(), dto.carrito()));
        mesaEstado.setTipo(dto.tipo() != null ? normalizarTipo(dto.tipo(), mesaId) : valorODefecto(mesaEstado.getTipo(), inferirTipo(mesaId)));
        mesaEstado.setVisible(dto.visible() != null ? dto.visible() : valorODefecto(mesaEstado.getVisible(), true));
        mesaEstado.setOrdenVisual(dto.ordenVisual() != null ? dto.ordenVisual() : valorODefecto(mesaEstado.getOrdenVisual(), ordenPorDefecto(mesaId)));
        if (dto.tipo() != null) {
            mesaEstado.setDomicilioDireccion(limpiar(dto.domicilioDireccion()));
            mesaEstado.setDomicilioCosto(dto.domicilioCosto() == null ? null : Math.max(0D, dto.domicilioCosto()));
            mesaEstado.setDomicilioNombreRecibe(limpiar(dto.domicilioNombreRecibe()));
            mesaEstado.setDomicilioCelularRecibe(limpiar(dto.domicilioCelularRecibe()));
        }
        mesaEstado.setFechaActualizacion(ahoraBogota());

        List<MesaEstadoItem> items = new ArrayList<>();
        for (MesaEstadoItemDTO itemDto : dto.carrito() == null ? List.<MesaEstadoItemDTO>of() : dto.carrito()) {
            MesaEstadoItem item = new MesaEstadoItem();
            item.setMesaEstado(mesaEstado);
            item.setNombreLibre(limpiar(itemDto.nombreLibre()));
            item.setPrecioUnitario(itemDto.precioUnitario() != null ? itemDto.precioUnitario() : 0D);
            item.setCantidad(itemDto.cantidad() != null ? itemDto.cantidad() : 0);
            item.setTotal(itemDto.total() != null ? itemDto.total() : 0D);

            if (itemDto.producto() != null) {
                item.setProductoId(itemDto.producto().productoId());
                item.setProductoNombre(itemDto.producto().productoNombre());
                item.setStockActual(itemDto.producto().stockActual());
                item.setEntradas(itemDto.producto().entradas());
                item.setSalidas(itemDto.producto().salidas());
                item.setPerdidas(itemDto.producto().perdidas());
                item.setStockMinimo(itemDto.producto().stockMinimo());
                item.setPrecioVenta(itemDto.producto().precioVenta());
            }

            items.add(item);
        }

        mesaEstado.getItems().clear();
        mesaEstado.getItems().addAll(items);

        MesaEstadoDTO guardada = mapToDto(mesaEstadoRepository.saveAndFlush(mesaEstado));
        eventPublisher.publishEvent(new MesaEstadoActualizadoEvento(sedeId, guardada));
        return guardada;
    }

    private MesaEstadoDTO mapToDto(MesaEstado mesaEstado) {
        return new MesaEstadoDTO(
                mesaEstado.getMesaReferenciaId(),
                mesaEstado.getNumero(),
                mesaEstado.getEstado(),
                mesaEstado.getItems().stream().map(item -> new MesaEstadoItemDTO(
                        item.getProductoId() != null || item.getProductoNombre() != null
                                ? new InventarioDTO(
                                null,
                                item.getProductoId(),
                                item.getProductoNombre(),
                                item.getStockActual(),
                                item.getEntradas(),
                                item.getSalidas(),
                                item.getPerdidas(),
                                item.getStockMinimo(),
                                item.getPrecioVenta()
                        )
                                : null,
                        item.getNombreLibre(),
                        item.getPrecioUnitario(),
                        item.getCantidad(),
                        item.getTotal()
                )).toList(),
                mesaEstado.getNombre(),
                mesaEstado.getTipo(),
                mesaEstado.getVisible(),
                mesaEstado.getOrdenVisual(),
                mesaEstado.getDomicilioDireccion(),
                mesaEstado.getDomicilioCosto(),
                mesaEstado.getDomicilioNombreRecibe(),
                mesaEstado.getDomicilioCelularRecibe(),
                mesaEstado.getVersion()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public void validarAccesoASede(String correo, String rol, Long sedeId) {
        validarAcceso(correo, rol, sedeId);
    }

    @Override
    @Transactional
    public void liberarMesaPorVenta(Long sedeId, Long mesaId) {
        if (sedeId == null || mesaId == null) return;
        mesaEstadoRepository.findDetalleBySedeIdAndMesaReferenciaId(sedeId, mesaId).ifPresent(mesa -> {
            mesa.getItems().clear();
            mesa.setEstado("LIBRE");
            mesa.setDomicilioDireccion(null);
            mesa.setDomicilioCosto(null);
            mesa.setDomicilioNombreRecibe(null);
            mesa.setDomicilioCelularRecibe(null);
            mesa.setFechaActualizacion(ahoraBogota());
            MesaEstadoDTO liberada = mapToDto(mesaEstadoRepository.saveAndFlush(mesa));
            eventPublisher.publishEvent(new MesaEstadoActualizadoEvento(sedeId, liberada));
        });
    }

    private void validarAcceso(String correo, String rol, Long sedeId) {
        if ("administrador".equalsIgnoreCase(rol)) {
            Administrador admin = administradorRepository.findByCorreoIgnoreCase(correo)
                    .orElseThrow(() -> new RuntimeException("Administrador no encontrado"));
            administradorAccesoService.validarAccesoASede(admin, sedeId);
            return;
        }

        if ("vendedor".equalsIgnoreCase(rol) || "produccion".equalsIgnoreCase(rol)) {
            Vendedor vendedor = vendedorRepository.findByCorreoIgnoreCase(correo)
                    .orElseThrow(() -> new RuntimeException("Vendedor no encontrado"));

            if (vendedor.getSede() == null || !sedeId.equals(vendedor.getSede().getId())) {
                throw new RuntimeException("No tiene permisos para acceder a la sede seleccionada");
            }
            return;
        }

        throw new RuntimeException("No tiene permisos para acceder a este recurso");
    }

    private String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private String normalizarEstado(String estado, List<MesaEstadoItemDTO> carrito) {
        boolean ocupada = carrito != null && !carrito.isEmpty();
        if (ocupada) {
            return "OCUPADA";
        }
        return "LIBRE";
    }

    private String normalizarTipo(String tipo, Long mesaId) {
        String normalizado = tipo.trim().toUpperCase();
        return switch (normalizado) {
            case "MOSTRADOR", "BARRA", "MESA", "DOMICILIO" -> normalizado;
            default -> inferirTipo(mesaId);
        };
    }

    private String inferirTipo(Long mesaId) {
        if (mesaId == 0L) return "MOSTRADOR";
        if (mesaId == 1L) return "BARRA";
        if (mesaId == 9999L || (mesaId >= 9991L && mesaId <= 9994L)) return "DOMICILIO";
        return "MESA";
    }

    private int ordenPorDefecto(Long mesaId) {
        if (mesaId == 0L) return 0;
        if (mesaId == 1L) return 1;
        if (mesaId >= 9991L && mesaId <= 9994L) return 14 + mesaId.intValue() - 9991;
        return mesaId.intValue();
    }

    private <T> T valorODefecto(T valor, T defecto) {
        return valor != null ? valor : defecto;
    }

    private LocalDateTime ahoraBogota() {
        return ZonedDateTime.now(ZoneId.of("America/Bogota")).toLocalDateTime();
    }
}
