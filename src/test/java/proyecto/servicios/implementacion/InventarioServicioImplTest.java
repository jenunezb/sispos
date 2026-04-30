package proyecto.servicios.implementacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import proyecto.entidades.Empresa;
import proyecto.entidades.Inventario;
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
import static org.mockito.Mockito.never;
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

    @Test
    void registrarEntradaDebePermitirProductoSinRecetaEnLaSedeAunqueExistaEnOtra() {
        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);

        Producto producto = new Producto();
        producto.setCodigo(70L);
        producto.setNombre("Jugo");
        producto.setEmpresa(empresa);

        Sede sede = new Sede();
        sede.setId(9L);
        sede.setEmpresa(empresa);

        Inventario inventario = new Inventario();
        inventario.setProducto(producto);
        inventario.setSede(sede);
        inventario.setEntradas(1);
        inventario.setStockActual(2);
        inventario.setSalidas(0);
        inventario.setPerdidas(0);
        inventario.setStockMinimo(0);
        inventario.setAlertaStockMinimoActiva(false);

        when(productoRepository.findById(70L)).thenReturn(Optional.of(producto));
        when(productoMateriaPrimaRepository.findByProductoCodigoAndMateriaPrimaSedeSedeId(70L, 9L)).thenReturn(java.util.List.of());
        when(inventarioRepository.findVisibleByProductoCodigoAndSedeId(70L, 9L)).thenReturn(Optional.of(inventario));

        inventarioServicio.registrarEntrada(70L, 9L, 3);

        assertEquals(4, inventario.getEntradas());
        assertEquals(5, inventario.getStockActual());
        verify(inventarioRepository).save(inventario);
        verify(notificacionStockMinimoService).evaluarYNotificar(inventario, 5);
        verify(sedeRepository, never()).findById(9L);
    }
}
