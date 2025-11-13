package proyecto.servicios.interfaces;
import java.util.List;
import proyecto.entidades.Producto;

public interface ProductoServicio {
    List<Producto> listarProductos();
    Producto obtenerPorId(Long id);
    Producto guardar(Producto producto);
    void eliminar(Long id);
}
