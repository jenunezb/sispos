package proyecto.servicios.interfaces;
import java.util.List;

import proyecto.dto.ProductoActualizarDTO;
import proyecto.dto.ProductoCrearDTO;
import proyecto.dto.ProductoDTO;

public interface ProductoServicio {

    ProductoDTO crearProducto(ProductoCrearDTO dto);

    ProductoDTO actualizarProducto(ProductoActualizarDTO dto);

    ProductoDTO obtenerProductoPorCodigo(Long codigo);

    List<ProductoDTO> listarProductos();

    List<ProductoDTO> listarProductosActivos();

    void eliminarProducto(Long codigo);
}
