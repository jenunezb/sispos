package proyecto.servicios.implementacion;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import proyecto.dto.ProductoActualizarDTO;
import proyecto.dto.ProductoCrearDTO;
import proyecto.dto.ProductoDTO;
import proyecto.entidades.Empresa;
import proyecto.entidades.Inventario;
import proyecto.entidades.Producto;
import proyecto.entidades.Sede;
import proyecto.repositorios.InventarioRepository;
import proyecto.repositorios.ProductoRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.servicios.interfaces.ProductoServicio;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoServicioImpl implements ProductoServicio {

    @Autowired
    private final ProductoRepository productoRepository;
    private final InventarioRepository inventarioRepository;
    private final SedeRepository sedeRepository;

    @Override
    public ProductoDTO crearProducto(ProductoCrearDTO dto) {
        // 🔹 Buscar sede
        Sede sede = sedeRepository.findById(dto.sedeId())
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        Empresa empresa = sede.getEmpresa();
        if (empresa == null) {
            throw new RuntimeException("La sede no tiene una empresa asociada");
        }

        Producto producto = new Producto();
        producto.setNombre(dto.nombre());
        producto.setDescripcion(dto.descripcion());
        producto.setPrecioProduccion(dto.precioProduccion());
        producto.setPrecioVenta(dto.precioVenta());
        producto.setCategoria(dto.categoria());
        producto.setEstado(true);
        producto.setEmpresa(empresa);

        Producto guardado = productoRepository.save(producto);

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
    @Transactional
    public void desactivarProducto(Long codigo) {

        Producto producto = productoRepository.findById(codigo)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        producto.setActivo(false);

        productoRepository.save(producto);
    }

    @Override
    public List<ProductoDTO> listarProductos(Long empresaNit) {

        return productoRepository.findByActivoTrueAndEmpresaNitOrderByCodigoAsc(empresaNit)
                .stream()
                .map(this::mapToDTO)
                .toList();
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
