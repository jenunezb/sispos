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
        return inventarioRepository.findBySedeIdOrderByProductoCodigoAsc(sedeId)
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

    @Transactional
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
                        .findByMateriaPrimaAndSede(pmp.getMateriaPrima(), sede)
                        .orElseThrow(() -> new RuntimeException(
                                "No hay " + pmp.getMateriaPrima().getNombre() + " en esta sede"
                        ));

                double mlNecesarios = pmp.getMlConsumidos() * cantidad;

                if (mpSede.getCantidadActualMl() < mlNecesarios) {
                    throw new RuntimeException(
                            "Materia prima insuficiente: " + pmp.getMateriaPrima().getNombre()
                    );
                }

                mpSede.setCantidadActualMl(
                        mpSede.getCantidadActualMl() - mlNecesarios
                );
            }

            inventario.setSalidas(inventario.getSalidas() + cantidad);
        } else {
            int stockDisponible = calcularStockReal(inventario);

            if (stockDisponible < cantidad) {
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
            int stockDisponible = calcularStockReal(inventario);

            if (stockDisponible < cantidad) {
                throw new RuntimeException("Stock insuficiente");
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
    public List<InventarioDelDia> obtenerInventarioDia(Long sedeId, LocalDateTime fechaInicio,
                                                       LocalDateTime fechaFin) {
        List<Inventario> inventarios = inventarioRepository.findBySedeIdOrderByProductoCodigoAsc(sedeId);

        // 🔹 Usar la fecha que viene del frontend
        List<Object[]> movimientos = movimientoRepository.resumenMovimientosDelDia(sedeId, fechaInicio, fechaFin);

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
            boolean tieneReceta = !p.getMateriasPrimas().isEmpty();

            List<ProductoMateriaPrima> receta =
                    productoMateriaPrimaRepository.findByProductoCodigo(p.getCodigo());

            int stockActual;

            if (!receta.isEmpty()) {
                stockActual = calcularStockDesdeMateriaPrima(p, sedeId);
            } else {
                stockActual = calcularStockReal(inv);
            }


            int stockInicial;
            int entradas = 0;
            int salidasManuales = 0;
            int perdidas = 0;
            int ventas = 0;

            if (!tieneReceta) {
                // Producto normal
                int[] datos = movimientosMap.getOrDefault(
                        p.getCodigo(), new int[]{0,0,0,0});
                ventas = datos[0];
                perdidas = datos[1];
                salidasManuales = datos[2];
                entradas = datos[3];
                stockInicial = stockActual - entradas + ventas + perdidas + salidasManuales;
            } else {
                // Producto con receta
                int[] datos = movimientosMap.getOrDefault(
                        p.getCodigo(), new int[]{0,0,0,0});
                ventas = datos[0]; // <--- aquí agregas
                perdidas = datos[1];
                salidasManuales = datos[2];
                entradas = datos[3];
                stockInicial = stockActual + ventas + perdidas + salidasManuales - entradas;
                // Ajusta según cómo quieras mostrar stock inicial
            }

            double precio = p.getPrecioVenta();

            System.out.println("🔍 Producto: " + p.getNombre());
            System.out.println("🔍 Código producto: " + p.getCodigo());
            System.out.println("🔍 Materias primas size: " + p.getMateriasPrimas().size());

            return new InventarioDelDia(
                    p.getCodigo(),
                    p.getNombre(),
                    stockInicial,
                    entradas,
                    salidasManuales,
                    perdidas,
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

        // 🔹 Si NO tiene receta → stock normal
        if (p.getMateriasPrimas().isEmpty()) {
            return inv.getStockActual();
        }

        Sede sede = inv.getSede();
        int maxUnidades = Integer.MAX_VALUE;

        for (ProductoMateriaPrima pmp : p.getMateriasPrimas()) {

            MateriaPrimaSede mpSede = materiaPrimaSedeRepository
                    .findByMateriaPrimaCodigoAndSedeId(
                            pmp.getMateriaPrima().getCodigo(),
                            sede.getId()
                    )
                    .orElse(null);

            if (mpSede == null) {
                System.out.println(
                        "❌ MP no encontrada en sede: "
                                + pmp.getMateriaPrima().getNombre()
                                + " | producto: " + p.getNombre()
                );
                return 0; // no se puede producir ni 1 unidad
            }

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

    private int calcularStockDesdeMateriaPrima(Producto producto, Long sedeId) {

        List<ProductoMateriaPrima> receta =
                productoMateriaPrimaRepository.findByProductoCodigo(producto.getCodigo());

        int stock = Integer.MAX_VALUE;

        for (ProductoMateriaPrima pmp : receta) {

            MateriaPrimaSede mpSede = materiaPrimaSedeRepository
                    .findByMateriaPrimaCodigoAndSedeId(
                            pmp.getMateriaPrima().getCodigo(),
                            sedeId
                    )
                    .orElse(null);

            if (mpSede == null) {
                return 0;
            }

            int unidades = (int) Math.floor(
                    mpSede.getCantidadActualMl() / pmp.getMlConsumidos()
            );

            stock = Math.min(stock, unidades);
        }

        return stock == Integer.MAX_VALUE ? 0 : stock;
    }

}

