package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import proyecto.dto.ProductoActualizarDTO;
import proyecto.dto.ProductoCrearDTO;
import proyecto.dto.ProductoDTO;
import proyecto.entidades.Inventario;
import proyecto.entidades.Producto;
import proyecto.entidades.Sede;
import proyecto.repositorios.InventarioRepository;
import proyecto.repositorios.ProductoRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.servicios.interfaces.ProductoServicio;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductoServicioImpl implements ProductoServicio {

    @Autowired
    private final ProductoRepository productoRepository;
    private final InventarioRepository inventarioRepository;
    private final SedeRepository sedeRepository;

    @Override
    public ProductoDTO crearProducto(ProductoCrearDTO dto) {
        Producto producto = new Producto();
        producto.setNombre(dto.nombre());
        producto.setDescripcion(dto.descripcion());
        producto.setPrecioProduccion(dto.precioProduccion());
        producto.setPrecioVenta(dto.precioVenta());
        producto.setCategoria(dto.categoria());
        producto.setEstado(true);

        Producto guardado = productoRepository.save(producto);

        // 🔹 Buscar sede
        Sede sede = sedeRepository.findById(dto.sedeId())
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        // 🔹 Crear inventario automáticamente
        Inventario inventario = new Inventario();
        inventario.setProducto(guardado);
        inventario.setSede(sede);
        inventario.setEntradas(0);
        inventario.setSalidas(0);
        inventario.setPerdidas(0);
        inventario.setStockActual(0);

        inventarioRepository.save(inventario);

        return mapToDTO(guardado);

    }

    @Override
    public ProductoDTO actualizarProducto(ProductoActualizarDTO dto) {
        Producto producto = productoRepository.findById(dto.codigo())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        producto.setNombre(dto.nombre());
        producto.setDescripcion(dto.descripcion());
        producto.setPrecioProduccion(dto.precioProduccion());
        producto.setPrecioVenta(dto.precioVenta());
        producto.setCategoria(dto.categoria());
        producto.setEstado(dto.estado());

        Producto actualizado = productoRepository.save(producto);
        return mapToDTO(actualizado);
    }

    @Override
    public ProductoDTO obtenerProductoPorCodigo(Long codigo) {
        Producto producto = productoRepository.findById(codigo)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        return mapToDTO(producto);
    }

    @Override
    public List<ProductoDTO> listarProductos() {
        return productoRepository.findAllByOrderByCodigoAsc()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }


    @Override
    public List<ProductoDTO> listarProductosActivos() {
        return productoRepository.findByEstadoTrue()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void eliminarProducto(Long codigo) {
        Producto producto = productoRepository.findById(codigo)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        producto.setEstado(false);
        productoRepository.save(producto);
    }

    private ProductoDTO mapToDTO(Producto producto) {
        return new ProductoDTO(
                producto.getCodigo(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecioProduccion(),
                producto.getPrecioVenta(),
                producto.getCategoria(),
                producto.getEstado()
        );
    }
}