package proyecto.servicios.implementacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import proyecto.entidades.Empresa;
import proyecto.entidades.Producto;
import proyecto.entidades.Sede;
import proyecto.repositorios.InventarioRepository;
import proyecto.repositorios.MateriaPrimaSedeRepository;
import proyecto.repositorios.MovimientoInventarioRepository;
import proyecto.repositorios.ProductoMateriaPrimaRepository;
import proyecto.repositorios.ProductoRepository;
import proyecto.repositorios.SedeRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventarioServicioImplTest {

    @Mock private InventarioRepository inventarioRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private SedeRepository sedeRepository;
    @Mock private MovimientoInventarioRepository movimientoRepository;
    @Mock private ProductoMateriaPrimaRepository productoMateriaPrimaRepository;
    @Mock private MateriaPrimaSedeRepository materiaPrimaSedeRepository;
    @Mock private NotificacionStockMinimoService notificacionStockMinimoService;

    @InjectMocks
    private InventarioServicioImpl inventarioServicio;

    @Test
    void registrarEntradaDebeRechazarProductoDeOtraEmpresa() {
        Empresa empresaProducto = new Empresa();
        empresaProducto.setNit(1007960474L);

        Empresa empresaSede = new Empresa();
        empresaSede.setNit(1097726190L);

        Producto producto = new Producto();
        producto.setCodigo(59L);
        producto.setNombre("Pastel de pollo");
        producto.setEmpresa(empresaProducto);

        Sede sede = new Sede();
        sede.setId(7L);
        sede.setEmpresa(empresaSede);

        when(inventarioRepository.findVisibleByProductoCodigoAndSedeId(59L, 7L)).thenReturn(Optional.empty());
        when(productoRepository.findById(59L)).thenReturn(Optional.of(producto));
        when(sedeRepository.findById(7L)).thenReturn(Optional.of(sede));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> inventarioServicio.registrarEntrada(59L, 7L, 10));

        assertEquals("El producto no pertenece a la empresa de la sede", error.getMessage());
        verify(inventarioRepository).findVisibleByProductoCodigoAndSedeId(59L, 7L);
    }
}
