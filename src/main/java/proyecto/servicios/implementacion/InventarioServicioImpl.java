package proyecto.servicios.implementacion;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import proyecto.dto.InventarioDTO;
import proyecto.entidades.Inventario;
import proyecto.entidades.Producto;
import proyecto.entidades.Sede;
import proyecto.repositorios.InventarioRepository;
import proyecto.repositorios.ProductoRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.servicios.interfaces.InventarioServicio;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InventarioServicioImpl implements InventarioServicio {

    private final InventarioRepository inventarioRepository;
    private final ProductoRepository productoRepository;
    private final SedeRepository sedeRepository;

    @Override
    @Transactional
    public List<InventarioDTO> listarPorSede(Long sedeId) {
        return inventarioRepository.findBySedeId(sedeId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public InventarioDTO obtenerPorProductoYSede(Long productoId, Long sedeId) {
        Inventario inventario = inventarioRepository
                .findByProductoCodigoAndSedeId(productoId, sedeId)
                .orElseThrow(() -> new RuntimeException("Inventario no encontrado"));

        return toDTO(inventario);
    }

    @Override
    public void registrarEntrada(Long productoId, Long sedeId, Integer cantidad) {
        Inventario inventario = obtenerOcrearInventario(productoId, sedeId);

        inventario.setEntradas(inventario.getEntradas() + cantidad);
        inventario.setStockActual(inventario.getStockActual() + cantidad);

        inventarioRepository.save(inventario);
    }

    @Override
    public void registrarSalida(Long productoId, Long sedeId, Integer cantidad) {
        Inventario inventario = obtenerOcrearInventario(productoId, sedeId);

        if (inventario.getStockActual() < cantidad) {
            throw new RuntimeException("Stock insuficiente");
        }

        inventario.setSalidas(inventario.getSalidas() + cantidad);
        inventario.setStockActual(inventario.getStockActual() - cantidad);

        inventarioRepository.save(inventario);
    }

    @Override
    public void registrarPerdida(Long productoId, Long sedeId, Integer cantidad) {
        Inventario inventario = obtenerOcrearInventario(productoId, sedeId);

        if (inventario.getStockActual() < cantidad) {
            throw new RuntimeException("Stock insuficiente para registrar pérdida");
        }

        inventario.setPerdidas(inventario.getPerdidas() + cantidad);
        inventario.setStockActual(inventario.getStockActual() - cantidad);

        inventarioRepository.save(inventario);
    }

    // ===============================
    // MÉTODOS PRIVADOS
    // ===============================

    private Inventario obtenerOcrearInventario(Long productoId, Long sedeId) {

        return inventarioRepository
                .findByProductoCodigoAndSedeId(productoId, sedeId)
                .orElseGet(() -> {
                    Producto producto = productoRepository.findById(productoId)
                            .orElseThrow(() -> new RuntimeException("Producto no existe"));

                    Sede sede = sedeRepository.findById(sedeId)
                            .orElseThrow(() -> new RuntimeException("Sede no existe"));

                    Inventario nuevo = new Inventario();
                    nuevo.setProducto(producto);
                    nuevo.setSede(sede);
                    nuevo.setStockActual(0);
                    nuevo.setEntradas(0);
                    nuevo.setSalidas(0);
                    nuevo.setPerdidas(0);

                    return inventarioRepository.save(nuevo);
                });
    }

    private InventarioDTO toDTO(Inventario inv) {
        return new InventarioDTO(
                inv.getId(),
                inv.getProducto().getCodigo(),
                inv.getProducto().getNombre(),
                inv.getStockActual(),
                inv.getEntradas(),
                inv.getSalidas(),
                inv.getPerdidas()
        );
    }
}
