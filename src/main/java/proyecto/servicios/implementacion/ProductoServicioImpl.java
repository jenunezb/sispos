package proyecto.servicios.implementacion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import proyecto.dto.ProductoActualizarDTO;
import proyecto.dto.ProductoCrearDTO;
import proyecto.dto.ProductoDTO;
import proyecto.entidades.Producto;
import proyecto.repositorios.ProductoRepository;
import proyecto.servicios.interfaces.ProductoServicio;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductoServicioImpl implements ProductoServicio {

    @Autowired
    private ProductoRepository productoRepository;

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
        return mapToDTO(guardado);
    }

    @Override
    public ProductoDTO actualizarProducto(ProductoActualizarDTO dto) {
        Producto producto = productoRepository.findById(dto.codigo())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        producto.setNombre(dto.nombre());
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
        return productoRepository.findAll()
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