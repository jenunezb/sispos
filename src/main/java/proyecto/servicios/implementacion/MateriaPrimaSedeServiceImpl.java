package proyecto.servicios.implementacion;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import proyecto.dto.*;
import proyecto.entidades.*;
import proyecto.repositorios.*;
import proyecto.servicios.interfaces.MateriaPrimaSedeService;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class MateriaPrimaSedeServiceImpl implements MateriaPrimaSedeService {

    private final MateriaPrimaSedeRepository materiaPrimaSedeRepository;
    private final MateriaPrimaRepository materiaPrimaRepository;
    private final ProductoRepository productoRepository;
    private final ProductoMateriaPrimaRepository productoMateriaPrimaRepository;
    private final SedeRepository sedeRepository;
    private final InventarioRepository inventarioRepository;


    @Override
    public void crearMateriaPrima(CrearMateriaPrimaDTO crearMateriaPrimaDTO) {

        if (materiaPrimaRepository.existsByNombreIgnoreCase(crearMateriaPrimaDTO.nombre())) {
            throw new IllegalArgumentException("La materia prima ya existe");
        }

        // 1️⃣ Crear materia prima
        MateriaPrima materiaPrima = new MateriaPrima();
        materiaPrima.setNombre(crearMateriaPrimaDTO.nombre());
        materiaPrima.setActiva(crearMateriaPrimaDTO.activa());

        materiaPrimaRepository.save(materiaPrima);

    }
    @Override
    @Transactional
    public void materiaPrimaSede(Long codigoMateriaPrima, Long codigoSede) {

        MateriaPrima materiaPrima = materiaPrimaRepository.findById(codigoMateriaPrima)
                .orElseThrow(() -> new RuntimeException("Materia prima no encontrada"));

        Sede sede = sedeRepository.findById(codigoSede)
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        // 🔒 Evitar duplicados
        if (materiaPrimaSedeRepository
                .existsByMateriaPrimaAndSede(materiaPrima, sede)) {
            throw new RuntimeException("La materia prima ya está asociada a esta sede");
        }

        // 🧩 Crear relación
        MateriaPrimaSede mps = new MateriaPrimaSede();
        mps.setMateriaPrima(materiaPrima);
        mps.setSede(sede);
        mps.setCantidadActualMl(0); // inicial

        materiaPrimaSedeRepository.save(mps);
    }

    @Transactional
    public MateriaPrimaSedeResponseDTO crearYVincular(@Valid CrearMateriaPrimaSedeDTO dto) {

        Sede sede = sedeRepository.findById(dto.sedeId())
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        MateriaPrima materiaPrima = materiaPrimaRepository
                .findByNombreIgnoreCaseAndEmpresaNit(dto.nombre(), sede.getEmpresa().getNit())
                .orElseGet(() -> {
                    MateriaPrima mp = new MateriaPrima();
                    mp.setNombre(dto.nombre());
                    mp.setActiva(dto.activa());
                    mp.setEmpresa(sede.getEmpresa());
                    return materiaPrimaRepository.save(mp);
                });

        if (materiaPrimaSedeRepository.existsByMateriaPrimaAndSedeId(
                materiaPrima, dto.sedeId())) {
            throw new RuntimeException("La materia prima ya está vinculada a esta sede");
        }

        MateriaPrimaSede mpSede = new MateriaPrimaSede();
        mpSede.setMateriaPrima(materiaPrima);
        mpSede.setSede(sede);
        mpSede.setCantidadActualMl(dto.cantidadInicialMl());
        mpSede.setMlPorVaso(0);
        mpSede.setActiva(true);

        materiaPrimaSedeRepository.save(mpSede);

        return new MateriaPrimaSedeResponseDTO(
                materiaPrima.getCodigo(),
                materiaPrima.getNombre(),
                sede.getId(),
                "Materia prima creada y vinculada correctamente"
        );
    }


    @Override
    public int calcularVasosDisponibles(Long materiaPrimaId, Long sedeId) {
        MateriaPrimaSede mpSede = materiaPrimaSedeRepository
                .findByMateriaPrimaCodigoAndSedeId(materiaPrimaId, sedeId)
                .orElseThrow(() -> new IllegalStateException(
                        "Materia prima no configurada en esta sede"
                ));
        return (int) (mpSede.getCantidadActualMl() / mpSede.getMlPorVaso());
    }
    @Override
    public void descontarPorVenta(Long materiaPrimaId, Long sedeId, int vasosVendidos) {
        MateriaPrimaSede mpSede = materiaPrimaSedeRepository
                .findByMateriaPrimaCodigoAndSedeId(materiaPrimaId, sedeId)
                .orElseThrow(() -> new IllegalStateException(
                        "Materia prima no configurada en esta sede"
                ));

        double mlADescontar = vasosVendidos * mpSede.getMlPorVaso();

        if (mlADescontar > mpSede.getCantidadActualMl()) {
            throw new IllegalStateException(
                    "No hay suficiente materia prima en la sede"
            );
        }

        mpSede.setCantidadActualMl(mpSede.getCantidadActualMl() - mlADescontar);
        materiaPrimaSedeRepository.save(mpSede);
    }
    @Override
    public ProductoMateriaPrimaRequestDTO vincularMateriaPrima(Long productoId, Long materiaPrimaId, double mlConsumidos) {
        if (mlConsumidos <= 0) {
            throw new IllegalArgumentException("El consumo del ingrediente debe ser mayor a cero");
        }


        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        MateriaPrima materiaPrima = materiaPrimaRepository.findById(materiaPrimaId)
                .orElseThrow(() -> new RuntimeException("Materia prima no encontrada"));

        // 🔒 Verificar duplicado antes de guardar
        boolean existe = productoMateriaPrimaRepository.existsByProductoAndMateriaPrima(producto, materiaPrima);
        if (existe) {
            throw new RuntimeException("La materia prima ya está vinculada a este producto");
        }

        // Crear la relación
        ProductoMateriaPrima pmp = new ProductoMateriaPrima();
        pmp.setProducto(producto);
        pmp.setMateriaPrima(materiaPrima);
        pmp.setMlConsumidos(mlConsumidos);

        productoMateriaPrimaRepository.save(pmp);

        return new ProductoMateriaPrimaRequestDTO(
                producto.getCodigo(),
                producto.getNombre(),
                materiaPrima.getCodigo(),
                materiaPrima.getNombre(),
                mlConsumidos
        );
    }
    @Override
    public List<MateriaPrimaSedeDTO> listarTodas() {
        return mapearMaterias(materiaPrimaSedeRepository.findAll());
    }

    @Override
    public List<MateriaPrimaSedeDTO> listarPorSedes(List<Long> sedeIds) {
        if (sedeIds == null || sedeIds.isEmpty()) {
            return List.of();
        }
        return mapearMaterias(materiaPrimaSedeRepository.findBySedeIdInOrderByIdAsc(sedeIds));
    }

    private List<MateriaPrimaSedeDTO> mapearMaterias(List<MateriaPrimaSede> materias) {
        return materias
                .stream()
                .map(mp -> new MateriaPrimaSedeDTO(
                        mp.getId(),
                        mp.getMateriaPrima().getCodigo(),
                        mp.getMateriaPrima().getNombre(),
                        mp.getMateriaPrima().isActiva(),

                        mp.getSede().getId(),
                        mp.getSede().getUbicacion(),

                        mp.getCantidadActualMl(),
                        0,
                        mp.isActiva()
                ))
                .toList();
    }

    /**
     * Actualizar cantidad y estado. El consumo real se configura por producto.
     */
    public void actualizarMateriaPrimaSede(Long id, MateriaPrimaSedeUpdate dto) {
        MateriaPrimaSede mpSede = materiaPrimaSedeRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "Materia prima en sede no encontrada"
                ));

        mpSede.setCantidadActualMl(dto.cantidad());
        mpSede.setActiva(dto.activa());

        materiaPrimaSedeRepository.save(mpSede);
    }

    @Override
    public void eliminarMateriaPrima(Long materiaPrimaId) {
        MateriaPrima materiaPrima = materiaPrimaRepository.findById(materiaPrimaId)
                .orElseThrow(() -> new IllegalStateException("Materia prima no encontrada"));

        productoMateriaPrimaRepository.deleteAllInBatch(
                productoMateriaPrimaRepository.findByMateriaPrimaCodigoOrderByProductoNombreAsc(materiaPrimaId)
        );
        materiaPrimaSedeRepository.deleteAllInBatch(
                materiaPrimaSedeRepository.findByMateriaPrimaCodigo(materiaPrimaId)
        );
        materiaPrimaRepository.delete(materiaPrima);
    }

    /**
     * Vincular un producto a una materia prima en una sede
     */
    @Transactional
    public void vincularProducto(VincularProductoDTO dto) {
        MateriaPrimaSede mpSede = materiaPrimaSedeRepository.findById(dto.materiaPrimaSedeId())
                .orElseThrow(() ->
                        new IllegalStateException("Materia prima de la sede no encontrada")
                );

        Producto producto = productoRepository.findById(dto.productoId())
                .orElseThrow(() ->
                        new IllegalStateException("Producto no encontrado")
                );

        boolean productoVisibleEnSede = inventarioRepository
                .findVisibleByProductoCodigoAndSedeId(producto.getCodigo(), mpSede.getSede().getId())
                .isPresent();
        if (!productoVisibleEnSede) {
            throw new IllegalStateException("El producto no pertenece a la sede seleccionada");
        }

        boolean existe = productoMateriaPrimaRepository
                .existsByMateriaPrimaIdAndProductoId(
                        mpSede.getMateriaPrima().getCodigo(),
                        dto.productoId()
                );

        if (existe) {
            throw new IllegalStateException(
                    "El producto ya está vinculado a esta materia prima en esta sede"
            );
        }

        // 🔹 Crear relación ProductoMateriaPrima
        ProductoMateriaPrima nueva = new ProductoMateriaPrima();
        nueva.setProducto(producto);
        nueva.setMateriaPrima(mpSede.getMateriaPrima());
        nueva.setMlConsumidos(dto.mlConsumidos());

        productoMateriaPrimaRepository.save(nueva);

        // 🔥 NUEVA LÓGICA: INVENTARIO A CERO
        inventarioRepository
                .findVisibleByProductoCodigoAndSedeId(
                        producto.getCodigo(),
                        mpSede.getSede().getId()
                )
                .ifPresent(inventario -> {
                    inventario.setStockActual(0);
                    inventario.setEntradas(0);
                    inventario.setSalidas(0);
                    inventario.setPerdidas(0);
                    inventarioRepository.save(inventario);
                });
    }

    @Override
    public void actualizarConsumoProducto(Long materiaPrimaSedeId, Long productoId, ActualizarConsumoProductoDTO dto) {
        MateriaPrimaSede mpSede = materiaPrimaSedeRepository.findById(materiaPrimaSedeId)
                .orElseThrow(() -> new IllegalStateException("Materia prima de la sede no encontrada"));

        inventarioRepository.findVisibleByProductoCodigoAndSedeId(productoId, mpSede.getSede().getId())
                .orElseThrow(() -> new IllegalStateException("El producto no pertenece a la sede seleccionada"));

        ProductoMateriaPrima relacion = productoMateriaPrimaRepository
                .findByMateriaPrimaCodigoAndProductoCodigo(mpSede.getMateriaPrima().getCodigo(), productoId)
                .orElseThrow(() -> new IllegalStateException("El producto no esta vinculado a esta materia prima"));

        relacion.setMlConsumidos(dto.mlConsumidos());
        productoMateriaPrimaRepository.save(relacion);
    }

    @Override
    public List<MateriaPrimaProductoDTO> listarProductosVinculados(Long materiaPrimaSedeId) {
        MateriaPrimaSede mpSede = materiaPrimaSedeRepository.findById(materiaPrimaSedeId)
                .orElseThrow(() -> new IllegalStateException("Materia prima de la sede no encontrada"));

        return productoMateriaPrimaRepository
                .findByMateriaPrimaCodigoAndSedeIdOrderByProductoNombreAsc(
                        mpSede.getMateriaPrima().getCodigo(),
                        mpSede.getSede().getId()
                )
                .stream()
                .map(relacion -> new MateriaPrimaProductoDTO(
                        relacion.getProducto().getCodigo(),
                        relacion.getProducto().getNombre(),
                        relacion.getMlConsumidos()
                ))
                .toList();
    }

    @Override
    public void desvincularProducto(Long materiaPrimaSedeId, Long productoId) {
        MateriaPrimaSede mpSede = materiaPrimaSedeRepository.findById(materiaPrimaSedeId)
                .orElseThrow(() -> new IllegalStateException("Materia prima de la sede no encontrada"));

        inventarioRepository.findVisibleByProductoCodigoAndSedeId(productoId, mpSede.getSede().getId())
                .orElseThrow(() -> new IllegalStateException("El producto no pertenece a la sede seleccionada"));

        ProductoMateriaPrima relacion = productoMateriaPrimaRepository
                .findByMateriaPrimaCodigoAndProductoCodigo(mpSede.getMateriaPrima().getCodigo(), productoId)
                .orElseThrow(() -> new IllegalStateException("El producto no está vinculado a esta materia prima"));

        productoMateriaPrimaRepository.delete(relacion);
    }

    @Override
    public List<IngredienteProductoDTO> listarIngredientesProducto(Long productoId) {
        if (!productoRepository.existsById(productoId)) {
            throw new IllegalStateException("Producto no encontrado");
        }

        return productoMateriaPrimaRepository.findByProductoCodigo(productoId)
                .stream()
                .map(relacion -> new IngredienteProductoDTO(
                        relacion.getMateriaPrima().getCodigo(),
                        relacion.getMateriaPrima().getNombre(),
                        relacion.getMlConsumidos()
                ))
                .toList();
    }

    @Override
    public List<CargaMateriaPrimaResultadoDTO> cargarMasivamente(CargaMateriaPrimaMasivaDTO dto) {
        Sede sede = sedeRepository.findById(dto.sedeId())
                .orElseThrow(() -> new IllegalStateException("Sede no encontrada"));
        if (sede.getEmpresa() == null) {
            throw new IllegalStateException("La sede no tiene empresa asociada");
        }

        java.util.Set<String> nombres = new java.util.HashSet<>();
        for (CargaMateriaPrimaItemDTO item : dto.items()) {
            String nombreNormalizado = item.nombre().trim().toLowerCase(Locale.ROOT);
            if (!nombres.add(nombreNormalizado)) {
                throw new IllegalArgumentException("La materia prima '" + item.nombre().trim() + "' está repetida en la carga");
            }
        }

        return dto.items().stream().map(item -> cargarItem(sede, item)).toList();
    }

    private CargaMateriaPrimaResultadoDTO cargarItem(Sede sede, CargaMateriaPrimaItemDTO item) {
        String unidad = normalizarUnidad(item.unidadBase());
        String nombre = item.nombre().trim();
        MateriaPrima materia = materiaPrimaRepository
                .findByNombreIgnoreCaseAndEmpresaNit(nombre, sede.getEmpresa().getNit())
                .orElse(null);
        boolean creada = materia == null;

        if (creada) {
            materia = new MateriaPrima();
            materia.setNombre(nombre);
            materia.setEmpresa(sede.getEmpresa());
            materia.setActiva(true);
            materia.setUnidadBase(unidad);
            materia.setCostoUnitario(0D);
            materia = materiaPrimaRepository.save(materia);
        } else {
            String unidadActual = materia.getUnidadBase();
            if (unidadActual != null && !unidadActual.equalsIgnoreCase(unidad)) {
                throw new IllegalArgumentException(
                        materia.getNombre() + " ya está configurada en " + unidadActual
                );
            }
        }

        double cantidadAgregada = item.cantidadPresentaciones() * item.contenidoPresentacion();
        double valorCompra = item.cantidadPresentaciones() * item.precioPresentacion();
        double costoCompraUnitario = item.precioPresentacion() / item.contenidoPresentacion();
        double stockAnteriorTotal = materiaPrimaSedeRepository.sumarStockMateriaPrima(materia.getCodigo());
        boolean tieneCostoAnterior = materia.getCostoUnitario() != null && materia.getCostoUnitario() > 0;
        double costoAnterior = tieneCostoAnterior ? materia.getCostoUnitario() : 0D;
        double costoPromedio = stockAnteriorTotal > 0 && tieneCostoAnterior
                ? ((stockAnteriorTotal * costoAnterior) + valorCompra) / (stockAnteriorTotal + cantidadAgregada)
                : costoCompraUnitario;

        materia.setUnidadBase(unidad);
        materia.setPresentacion(item.presentacion().trim());
        materia.setContenidoPresentacion(item.contenidoPresentacion());
        materia.setPrecioPresentacion(item.precioPresentacion());
        materia.setCostoUnitario(costoPromedio);
        materiaPrimaRepository.save(materia);

        MateriaPrimaSede materiaSede = materiaPrimaSedeRepository
                .findByMateriaPrimaCodigoAndSedeId(materia.getCodigo(), sede.getId())
                .orElse(null);
        if (materiaSede == null) {
            materiaSede = new MateriaPrimaSede();
            materiaSede.setMateriaPrima(materia);
            materiaSede.setSede(sede);
            materiaSede.setActiva(true);
            materiaSede.setCantidadActualMl(0);
            materiaSede.setMlPorVaso(0);
        }
        materiaSede.setCantidadActualMl(materiaSede.getCantidadActualMl() + cantidadAgregada);
        materiaSede.setActiva(true);
        materiaPrimaSedeRepository.save(materiaSede);

        return new CargaMateriaPrimaResultadoDTO(
                materia.getCodigo(), materia.getNombre(), creada, unidad,
                cantidadAgregada, materiaSede.getCantidadActualMl(), costoPromedio
        );
    }

    private String normalizarUnidad(String unidad) {
        String normalizada = unidad.trim().toUpperCase(Locale.ROOT);
        if (!java.util.Set.of("UNIDAD", "GRAMO", "ML").contains(normalizada)) {
            throw new IllegalArgumentException("Unidad de materia prima inválida: " + unidad);
        }
        return normalizada;
    }

    @Override
    public void actualizarIngredienteProducto(
            Long productoId,
            Long materiaPrimaId,
            ActualizarConsumoProductoDTO dto
    ) {
        if (dto.mlConsumidos() <= 0) {
            throw new IllegalArgumentException("El consumo del ingrediente debe ser mayor a cero");
        }

        ProductoMateriaPrima relacion = productoMateriaPrimaRepository
                .findByMateriaPrimaCodigoAndProductoCodigo(materiaPrimaId, productoId)
                .orElseThrow(() -> new IllegalStateException("El ingrediente no pertenece al producto"));

        relacion.setMlConsumidos(dto.mlConsumidos());
        productoMateriaPrimaRepository.save(relacion);
    }

    @Override
    public void eliminarIngredienteProducto(Long productoId, Long materiaPrimaId) {
        ProductoMateriaPrima relacion = productoMateriaPrimaRepository
                .findByMateriaPrimaCodigoAndProductoCodigo(materiaPrimaId, productoId)
                .orElseThrow(() -> new IllegalStateException("El ingrediente no pertenece al producto"));

        productoMateriaPrimaRepository.delete(relacion);
    }
}


