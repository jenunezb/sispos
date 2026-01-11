package proyecto.servicios.implementacion;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import proyecto.dto.InventarioDTO;
import proyecto.dto.InventarioDelDia;
import proyecto.dto.MovimientoInventarioDTO;
import proyecto.dto.PerdidasDetalleDTO;
import proyecto.entidades.*;
import proyecto.repositorios.*;
import proyecto.servicios.interfaces.InventarioServicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class InventarioServicioImpl implements InventarioServicio {

    private final InventarioRepository inventarioRepository;
    private final ProductoRepository productoRepository;
    private final SedeRepository sedeRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final ProductoMateriaPrimaRepository productoMateriaPrimaRepository;
    private final MateriaPrimaSedeRepository materiaPrimaSedeRepository;

    // ===============================
    // LISTADOS
    // ===============================

    @Override
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

    // ===============================
    // ENTRADAS
    // ===============================

    @Override
    public void registrarEntrada(Long productoId, Long sedeId, Integer cantidad) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no existe"));

        if (!producto.getMateriasPrimas().isEmpty()) {
            throw new RuntimeException(
                    "No se permiten entradas manuales a productos con materia prima"
            );
        }

        Inventario inventario = obtenerOcrearInventario(productoId, sedeId);
        inventario.setEntradas(inventario.getEntradas() + cantidad);
        inventario.setStockActual(inventario.getStockActual() + cantidad);

        inventarioRepository.save(inventario);
    }

    // ===============================
    // SALIDAS / VENTAS
    // ===============================

    @Override
    public void registrarSalida(Long productoId, Long sedeId, Integer cantidad, String observacion) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no existe"));

        Inventario inventario = obtenerOcrearInventario(productoId, sedeId);
        Sede sede = inventario.getSede();

        if (!producto.getMateriasPrimas().isEmpty()) {
            // validar y descontar materias primas por sede
            for (ProductoMateriaPrima pmp : producto.getMateriasPrimas()) {
                MateriaPrimaSede mpSede = materiaPrimaSedeRepository
                        .findByMateriaPrimaAndSede(pmp.getMateriaPrima(), sede)
                        .orElseThrow(() -> new RuntimeException(
                                "No hay " + pmp.getMateriaPrima().getNombre() + " en esta sede"
                        ));
                double mlNecesarios = pmp.getMlConsumidos() * cantidad;
                if (mpSede.getCantidadActualMl() < mlNecesarios) {
                    throw new RuntimeException("Materia prima insuficiente: " + mpSede.getMateriaPrima().getNombre());
                }
            }
            for (ProductoMateriaPrima pmp : producto.getMateriasPrimas()) {
                MateriaPrimaSede mpSede = materiaPrimaSedeRepository
                        .findByMateriaPrimaAndSede(pmp.getMateriaPrima(), sede).get();
                mpSede.setCantidadActualMl(mpSede.getCantidadActualMl() - pmp.getMlConsumidos() * cantidad);
            }
            inventario.setSalidas(inventario.getSalidas() + cantidad);
        } else {
            if (inventario.getStockActual() < cantidad) {
                throw new RuntimeException("Stock insuficiente");
            }
            inventario.setSalidas(inventario.getSalidas() + cantidad);
            inventario.setStockActual(inventario.getStockActual() - cantidad);
        }

        inventarioRepository.save(inventario);

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setProducto(producto);
        movimiento.setSede(sede);
        movimiento.setTipo(TipoMovimiento.SALIDA);
        movimiento.setCantidad(cantidad);
        movimiento.setObservacion(observacion);
        movimientoRepository.save(movimiento);
    }

    // ===============================
    // PÉRDIDAS
    // ===============================

    @Override
    public void registrarPerdida(Long productoId, Long sedeId, Integer cantidad) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no existe"));

        Inventario inventario = obtenerOcrearInventario(productoId, sedeId);
        Sede sede = inventario.getSede();

        if (!producto.getMateriasPrimas().isEmpty()) {
            // validar y descontar materias primas
            for (ProductoMateriaPrima pmp : producto.getMateriasPrimas()) {
                MateriaPrimaSede mpSede = materiaPrimaSedeRepository
                        .findByMateriaPrimaAndSede(pmp.getMateriaPrima(), sede)
                        .orElseThrow(() -> new RuntimeException(
                                "No hay " + pmp.getMateriaPrima().getNombre() + " en esta sede"
                        ));
                double mlPerdidos = pmp.getMlConsumidos() * cantidad;
                if (mpSede.getCantidadActualMl() < mlPerdidos) {
                    throw new RuntimeException("Materia prima insuficiente: " + mpSede.getMateriaPrima().getNombre());
                }
            }
            for (ProductoMateriaPrima pmp : producto.getMateriasPrimas()) {
                MateriaPrimaSede mpSede = materiaPrimaSedeRepository
                        .findByMateriaPrimaAndSede(pmp.getMateriaPrima(), sede).get();
                mpSede.setCantidadActualMl(mpSede.getCantidadActualMl() - pmp.getMlConsumidos() * cantidad);
            }
            inventario.setPerdidas(inventario.getPerdidas() + cantidad);
        } else {
            if (inventario.getStockActual() < cantidad) {
                throw new RuntimeException("Stock insuficiente para pérdida");
            }
            inventario.setPerdidas(inventario.getPerdidas() + cantidad);
            inventario.setStockActual(inventario.getStockActual() - cantidad);
        }

        inventarioRepository.save(inventario);
    }

    // ===============================
    // MOVIMIENTOS MANUALES
    // ===============================

    @Override
    public void registrarMovimiento(MovimientoInventarioDTO dto) {
        Producto producto = productoRepository.findById(dto.productoId())
                .orElseThrow(() -> new RuntimeException("Producto no existe"));

        if (!producto.getMateriasPrimas().isEmpty() && dto.tipo() == TipoMovimiento.ENTRADA) {
            throw new RuntimeException("No se permiten entradas manuales a productos con materia prima");
        }

        Sede sede = sedeRepository.findById(dto.sedeId())
                .orElseThrow(() -> new RuntimeException("Sede no existe"));

        Inventario inventario = obtenerOcrearInventario(dto.productoId(), dto.sedeId());

        switch (dto.tipo()) {
            case ENTRADA -> {
                inventario.setEntradas(inventario.getEntradas() + dto.cantidad());
                inventario.setStockActual(inventario.getStockActual() + dto.cantidad());
            }
            case SALIDA -> {
                if (inventario.getStockActual() < dto.cantidad()) throw new RuntimeException("Stock insuficiente");
                inventario.setSalidas(inventario.getSalidas() + dto.cantidad());
                inventario.setStockActual(inventario.getStockActual() - dto.cantidad());
            }
            case PERDIDA -> {
                if (inventario.getStockActual() < dto.cantidad()) throw new RuntimeException("Stock insuficiente");
                inventario.setPerdidas(inventario.getPerdidas() + dto.cantidad());
                inventario.setStockActual(inventario.getStockActual() - dto.cantidad());
            }
        }

        inventarioRepository.save(inventario);

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setProducto(producto);
        movimiento.setSede(sede);
        movimiento.setTipo(dto.tipo());
        movimiento.setCantidad(dto.cantidad());
        movimiento.setObservacion(dto.observacion());
        movimientoRepository.save(movimiento);
    }

    // ===============================
    // INVENTARIO DEL DÍA
    // ===============================

    @Override
    public List<InventarioDelDia> obtenerInventarioDia(Long sedeId, LocalDateTime fecha) {
        List<Inventario> inventarios = inventarioRepository.findBySedeId(sedeId);
        LocalDate today = LocalDate.now();
        LocalDateTime inicio = today.atStartOfDay();
        LocalDateTime fin = today.atTime(23, 59, 59, 999_999_999);

        List<Object[]> movimientos = movimientoRepository.resumenMovimientosDelDia(sedeId, inicio, fin);
        Map<Long, int[]> movimientosMap = new HashMap<>();
        for (Object[] row : movimientos) {
            movimientosMap.put((Long) row[0], new int[]{
                    ((Number) row[1]).intValue(),
                    ((Number) row[2]).intValue(),
                    ((Number) row[3]).intValue(),
                    ((Number) row[4]).intValue()
            });
        }

        return inventarios.stream().map(inv -> {
            Producto p = inv.getProducto();
            int stockActual = calcularStockReal(inv);
            int[] datos = movimientosMap.getOrDefault(p.getCodigo(), new int[]{0, 0, 0, 0});
            int ventas = datos[0];
            int perdidas = datos[1];
            int salidasManuales = datos[2];
            int entradas = datos[3];
            int stockInicial = stockActual + ventas + perdidas + salidasManuales;
            double precio = p.getPrecioVenta();

            return new InventarioDelDia(
                    p.getCodigo(),
                    p.getNombre(),
                    stockInicial,
                    entradas,
                    ventas,
                    stockActual,
                    precio,
                    ventas * precio
            );
        }).toList();
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
        int stockReal = calcularStockReal(inv);
        return new InventarioDTO(
                inv.getId(),
                inv.getProducto().getCodigo(),
                inv.getProducto().getNombre(),
                stockReal,
                inv.getEntradas(),
                inv.getSalidas(),
                inv.getPerdidas(),
                inv.getProducto().getPrecioVenta()
        );
    }

    private int calcularStockReal(Inventario inv) {
        Producto p = inv.getProducto();
        if (p.getMateriasPrimas().isEmpty()) return inv.getStockActual();

        int maxUnidades = Integer.MAX_VALUE;
        Sede sede = inv.getSede();
        for (ProductoMateriaPrima pmp : p.getMateriasPrimas()) {
            MateriaPrimaSede mpSede = materiaPrimaSedeRepository
                    .findByMateriaPrimaAndSede(pmp.getMateriaPrima(), sede)
                    .orElseThrow();
            int posibles = (int) (mpSede.getCantidadActualMl() / pmp.getMlConsumidos());
            maxUnidades = Math.min(maxUnidades, posibles);
        }
        return maxUnidades;
    }

    // ===============================
    // REPORTES
    // ===============================

    @Override
    public List<PerdidasDetalleDTO> obtenerPerdidasDetalladasPorRango(Long sedeId, LocalDateTime inicio, LocalDateTime fin) {
        return inventarioRepository.obtenerPerdidasDetalladasPorRango(sedeId, inicio, fin);
    }
}

