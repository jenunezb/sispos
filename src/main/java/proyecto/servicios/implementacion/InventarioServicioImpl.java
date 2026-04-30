package proyecto.servicios.implementacion;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import proyecto.dto.*;
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
    private final NotificacionStockMinimoService notificacionStockMinimoService;

    // ===============================
    // LISTADOS
    // ===============================

    @Override
    public List<InventarioDTO> listarPorSede(Long sedeId) {
        return inventarioRepository.findVisiblesBySedeIdOrderByProductoCodigoAsc(sedeId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public InventarioDTO obtenerPorProductoYSede(Long productoId, Long sedeId) {
        Inventario inventario = inventarioRepository
                .findVisibleByProductoCodigoAndSedeId(productoId, sedeId)
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

        if (tieneRecetaEnSede(productoId, sedeId)) {
            throw new RuntimeException(
                    "No se permiten entradas manuales a productos con materia prima"
            );
        }

        Inventario inventario = obtenerOcrearInventario(productoId, sedeId);
        inventario.setEntradas(inventario.getEntradas() + cantidad);
        inventario.setStockActual(inventario.getStockActual() + cantidad);

        inventarioRepository.save(inventario);
        notificacionStockMinimoService.evaluarYNotificar(inventario, calcularStockReal(inventario));
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
        List<ProductoMateriaPrima> receta = obtenerRecetaProductoEnSede(productoId, sedeId);

        if (!receta.isEmpty()) {
            // validar y descontar materias primas por sede
            for (ProductoMateriaPrima pmp : receta) {
                MateriaPrimaSede mpSede = pmp.getMateriaPrimaSede();
                double mlNecesarios = pmp.getMlConsumidos() * cantidad;
                if (mpSede.getCantidadActualMl() < mlNecesarios) {
                    throw new RuntimeException("Materia prima insuficiente: " + mpSede.getMateriaPrima().getNombre());
                }
            }
            for (ProductoMateriaPrima pmp : receta) {
                MateriaPrimaSede mpSede = pmp.getMateriaPrimaSede();

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
        notificacionStockMinimoService.evaluarYNotificar(inventario, calcularStockReal(inventario));
    }

    // ===============================
    // PERDIDAS
    // ===============================

    @Override
    public void registrarPerdida(Long productoId, Long sedeId, Integer cantidad) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no existe"));

        Inventario inventario = obtenerOcrearInventario(productoId, sedeId);
        Sede sede = inventario.getSede();
        List<ProductoMateriaPrima> receta = obtenerRecetaProductoEnSede(productoId, sedeId);

        if (!receta.isEmpty()) {
            // validar y descontar materias primas
            for (ProductoMateriaPrima pmp : receta) {
                MateriaPrimaSede mpSede = pmp.getMateriaPrimaSede();
                double mlPerdidos = pmp.getMlConsumidos() * cantidad;
                if (mpSede.getCantidadActualMl() < mlPerdidos) {
                    throw new RuntimeException("Materia prima insuficiente: " + mpSede.getMateriaPrima().getNombre());
                }
            }
            for (ProductoMateriaPrima pmp : receta) {
                MateriaPrimaSede mpSede = pmp.getMateriaPrimaSede();
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
        notificacionStockMinimoService.evaluarYNotificar(inventario, calcularStockReal(inventario));
    }

    // ===============================
    // MOVIMIENTOS MANUALES
    // ===============================

    @Override
    public void registrarMovimiento(MovimientoInventarioDTO dto) {
        Producto producto = productoRepository.findById(dto.productoId())
                .orElseThrow(() -> new RuntimeException("Producto no existe"));

        if (tieneRecetaEnSede(dto.productoId(), dto.sedeId()) && dto.tipo() == TipoMovimiento.ENTRADA) {
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

                // ?? PRODUCTO CON RECETA (vasos, preparados, etc.)
                List<ProductoMateriaPrima> receta = obtenerRecetaProductoEnSede(dto.productoId(), dto.sedeId());
                if (!receta.isEmpty()) {

                    // 1?? Validar materia prima suficiente
                    for (ProductoMateriaPrima pmp : receta) {
                        MateriaPrimaSede mpSede = pmp.getMateriaPrimaSede();

                        double mlNecesarios = pmp.getMlConsumidos() * dto.cantidad();

                        if (mpSede.getCantidadActualMl() < mlNecesarios) {
                            throw new RuntimeException(
                                    "Materia prima insuficiente: " + pmp.getMateriaPrima().getNombre()
                            );
                        }
                    }

                    // 2?? Descontar materia prima
                    for (ProductoMateriaPrima pmp : receta) {
                        MateriaPrimaSede mpSede = pmp.getMateriaPrimaSede();

                        double mlNecesarios = pmp.getMlConsumidos() * dto.cantidad();

                        mpSede.setCantidadActualMl(
                                mpSede.getCantidadActualMl() - mlNecesarios
                        );
                    }

                    // 3?? Registrar perdida (solo historico)
                    inventario.setPerdidas(
                            inventario.getPerdidas() + dto.cantidad()
                    );

                    // ? NO se toca stockActual

                }
                // ?? PRODUCTO NORMAL (sin receta)
                else {

                    if (inventario.getStockActual() < dto.cantidad()) {
                        throw new RuntimeException("Stock insuficiente");
                    }

                    inventario.setPerdidas(
                            inventario.getPerdidas() + dto.cantidad()
                    );

                    inventario.setStockActual(
                            inventario.getStockActual() - dto.cantidad()
                    );
                }
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
        notificacionStockMinimoService.evaluarYNotificar(inventario, calcularStockReal(inventario));
    }

    // ===============================
    // INVENTARIO DEL DIA
    // ===============================

    @Override
    public void actualizarStockMinimo(Long productoId, Long sedeId, Integer stockMinimo) {
        if (stockMinimo == null || stockMinimo < 0) {
            throw new IllegalArgumentException("El stock minimo debe ser mayor o igual a 0");
        }

        Inventario inventario = obtenerOcrearInventario(productoId, sedeId);
        inventario.setStockMinimo(stockMinimo);

        if (stockMinimo == 0) {
            inventario.setAlertaStockMinimoActiva(false);
        }

        inventarioRepository.save(inventario);
        notificacionStockMinimoService.evaluarYNotificar(inventario, calcularStockReal(inventario));
    }

    @Override
    public List<InventarioDelDia> obtenerInventarioDia(
            Long sedeId,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin
    ) {

        // ===============================
        // 1?? INVENTARIO BASE POR SEDE
        // ===============================
        List<Inventario> inventarios =
                inventarioRepository.findVisiblesBySedeIdOrderByProductoCodigoAsc(sedeId);

        // ===============================
        // 2?? MOVIMIENTOS DEL DIA
        // [productoId, ventas, perdidas, salidasManuales, entradas]
        // ===============================
        List<Object[]> movimientos =
                movimientoRepository.resumenMovimientosDelDia(
                        sedeId, fechaInicio, fechaFin
                );

        Map<Long, int[]> movimientosMap = new HashMap<>();

        for (Object[] row : movimientos) {
            movimientosMap.put(
                    (Long) row[0],
                    new int[]{
                            ((Number) row[1]).intValue(), // ventas
                            ((Number) row[2]).intValue(), // perdidas
                            ((Number) row[3]).intValue(), // salidas manuales
                            ((Number) row[4]).intValue()  // entradas
                    }
            );
        }

        // ===============================
        // 3?? ARMAR INVENTARIO DEL DIA
        // ===============================
        return inventarios.stream().map(inv -> {

            Producto producto = inv.getProducto();
            Long codigoProducto = producto.getCodigo();

            boolean tieneReceta = tieneRecetaEnSede(codigoProducto, sedeId);

            // -------------------------------
            // MOVIMIENTOS DEL PRODUCTO
            // -------------------------------
            int[] datos = movimientosMap.getOrDefault(
                    codigoProducto,
                    new int[]{0, 0, 0, 0}
            );

            int ventas = datos[0];
            int perdidas = datos[1];
            int salidasManuales = datos[2];
            int entradas = datos[3];

            // -------------------------------
            // STOCK ACTUAL
            // -------------------------------
            int stockActual;

            if (tieneReceta) {
                stockActual = calcularStockDesdeMateriaPrima(
                        producto, sedeId
                );
            } else {
                stockActual = calcularStockReal(inv);
            }

            // -------------------------------
            // STOCK INICIAL
            // -------------------------------
            int stockInicial;

            if (tieneReceta) {
                // Producto elaborado
                stockInicial = stockActual
                        + ventas
                        + perdidas
                        + salidasManuales
                        - entradas;
            } else {
                // Producto normal
                stockInicial = stockActual
                        - entradas
                        + ventas
                        + perdidas
                        + salidasManuales;
            }

            // -------------------------------
            // PRECIO Y TOTAL
            // -------------------------------
            double precio = producto.getPrecioVenta();
            double totalVendido = ventas * precio;

            return new InventarioDelDia(
                    codigoProducto,
                    producto.getNombre(),
                    stockInicial,
                    entradas,
                    salidasManuales,
                    perdidas,
                    ventas,
                    stockActual,
                    precio,
                    totalVendido
            );

        }).toList();
    }


    // ===============================
    // METODOS PRIVADOS
    // ===============================

    private Inventario obtenerOcrearInventario(Long productoId, Long sedeId) {
        return inventarioRepository
                .findVisibleByProductoCodigoAndSedeId(productoId, sedeId)
                .orElseGet(() -> {
                    Producto producto = productoRepository.findById(productoId)
                            .orElseThrow(() -> new RuntimeException("Producto no existe"));
                    Sede sede = sedeRepository.findById(sedeId)
                            .orElseThrow(() -> new RuntimeException("Sede no existe"));
                    if (producto.getEmpresa() == null
                            || sede.getEmpresa() == null
                            || !producto.getEmpresa().getNit().equals(sede.getEmpresa().getNit())) {
                        throw new RuntimeException("El producto no pertenece a la empresa de la sede");
                    }
                    Inventario nuevo = new Inventario();
                    nuevo.setProducto(producto);
                    nuevo.setSede(sede);
                    nuevo.setStockActual(0);
                    nuevo.setEntradas(0);
                    nuevo.setSalidas(0);
                    nuevo.setPerdidas(0);
                    nuevo.setStockMinimo(0);
                    nuevo.setAlertaStockMinimoActiva(false);
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
                inv.getStockMinimo(),
                inv.getProducto().getPrecioVenta()
        );
    }

    private int calcularStockReal(Inventario inv) {
        Producto p = inv.getProducto();
        List<ProductoMateriaPrima> receta = obtenerRecetaProductoEnSede(
                p.getCodigo(),
                inv.getSede().getId()
        );

        // ?? Si NO tiene receta ? stock normal
        if (receta.isEmpty()) {
            return inv.getStockActual();
        }

        Sede sede = inv.getSede();
        int maxUnidades = Integer.MAX_VALUE;

        for (ProductoMateriaPrima pmp : receta) {
            MateriaPrimaSede mpSede = pmp.getMateriaPrimaSede();

            if (mpSede == null) {
                System.out.println(
                        "? MP no encontrada en sede: "
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
                obtenerRecetaProductoEnSede(producto.getCodigo(), sedeId);

        int stock = Integer.MAX_VALUE;

        for (ProductoMateriaPrima pmp : receta) {

            MateriaPrimaSede mpSede = pmp.getMateriaPrimaSede();

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

    @Override
    public List<MateriaPrimaInventarioDTO> obtenerInventarioMateriaPrimaDia(
            Long sedeId,
            LocalDateTime inicio,
            LocalDateTime fin
    ) {

        List<MateriaPrimaSede> materias = materiaPrimaSedeRepository.findBySedeIdOrderByIdAsc(sedeId);

        return materias.stream().map(mpSede -> {

            MateriaPrima mp = mpSede.getMateriaPrima();

            double stockActual = mpSede.getCantidadActualMl();

            double entradas = 0;   // aun no existen
            double perdidas = 0;   // aun no existen

            double vendidas = calcularConsumoPorVentas(
                    mp.getCodigo(),
                    sedeId,
                    inicio,
                    fin
            );

            double stockInicial = stockActual + vendidas;

            return new MateriaPrimaInventarioDTO(
                    mp.getCodigo(),
                    mp.getNombre(),
                    stockInicial,
                    entradas,
                    0,
                    perdidas,
                    vendidas,
                    stockActual
            );

        }).toList();
    }

    private double calcularConsumoPorVentas(
            Long materiaPrimaCodigo,
            Long sedeId,
            LocalDateTime inicio,
            LocalDateTime fin
    ) {

        // ?? Traer ventas por producto en el dia
        List<Object[]> ventas = movimientoRepository
                .resumenMovimientosDelDia(sedeId, inicio, fin);

        double totalConsumido = 0;

        for (Object[] row : ventas) {
            Long productoId = (Long) row[0];
            int cantidadVendida = ((Number) row[1]).intValue(); // ventas

            if (cantidadVendida <= 0) continue;

            // ?? Buscar receta del producto
            List<ProductoMateriaPrima> receta =
                    obtenerRecetaProductoEnSede(productoId, sedeId);

            for (ProductoMateriaPrima pmp : receta) {
                if (pmp.getMateriaPrima().getCodigo().equals(materiaPrimaCodigo)) {
                    totalConsumido += pmp.getMlConsumidos() * cantidadVendida;
                }
            }
        }

        return totalConsumido;
    }

    private List<ProductoMateriaPrima> obtenerRecetaProductoEnSede(Long productoId, Long sedeId) {
        return productoMateriaPrimaRepository.findByProductoCodigoAndMateriaPrimaSedeSedeId(productoId, sedeId);
    }

    private boolean tieneRecetaEnSede(Long productoId, Long sedeId) {
        return !obtenerRecetaProductoEnSede(productoId, sedeId).isEmpty();
    }

    @Override
    public void registrarMovimientoMateriaPrima(MovimientoMateriaPrimaDTO dto) {

        if (dto.cantidad() <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }

        MateriaPrimaSede mpSede = materiaPrimaSedeRepository
                .findByMateriaPrimaCodigoAndSedeId(
                        dto.materiaPrimaId(),
                        dto.sedeId()
                )
                .orElseThrow(() ->
                        new RuntimeException("Materia prima no encontrada para la sede")
                );

        double stockActual = mpSede.getCantidadActualMl(); // ? AQUI

        if ("ENTRADA".equals(dto.tipo())) {
            mpSede.setCantidadActualMl(stockActual + dto.cantidad());
        } else if ("SALIDA".equals(dto.tipo())) {

            if (stockActual < dto.cantidad()) {
                throw new RuntimeException("Stock insuficiente");
            }

            mpSede.setCantidadActualMl(stockActual - dto.cantidad());
        } else {
            throw new IllegalArgumentException("Tipo de movimiento invalido");
        }

        materiaPrimaSedeRepository.save(mpSede);
    }


}



