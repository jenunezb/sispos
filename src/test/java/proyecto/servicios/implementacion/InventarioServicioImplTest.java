package proyecto.servicios.implementacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import proyecto.dto.AjusteManualItemDTO;
import proyecto.dto.TipoInventarioAjustable;
import proyecto.entidades.Empresa;
import proyecto.entidades.Inventario;
import proyecto.entidades.MateriaPrima;
import proyecto.entidades.MateriaPrimaSede;
import proyecto.entidades.Producto;
import proyecto.entidades.ProductoMateriaPrima;
import proyecto.entidades.Sede;
import proyecto.repositorios.InventarioRepository;
import proyecto.repositorios.MateriaPrimaSedeRepository;
import proyecto.repositorios.MovimientoInventarioRepository;
import proyecto.repositorios.ProductoMateriaPrimaRepository;
import proyecto.repositorios.ProductoRepository;
import proyecto.repositorios.SedeRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
    void listarInventarioAjustablePorSedeDebeCombinarProductosSueltosYMateriasPrimas() {
        Empresa empresa = new Empresa();
        empresa.setNit(1007960474L);

        Producto producto = new Producto();
        producto.setCodigo(1L);
        producto.setNombre("Gaseosa 400ml");
        producto.setEmpresa(empresa);

        Inventario inventario = new Inventario();
        inventario.setProducto(producto);
        inventario.setStockActual(12);

        MateriaPrima materiaPrima = new MateriaPrima();
        materiaPrima.setCodigo(8L);
        materiaPrima.setNombre("Harina");
        materiaPrima.setActiva(true);

        MateriaPrimaSede materiaPrimaSede = new MateriaPrimaSede();
        materiaPrimaSede.setMateriaPrima(materiaPrima);
        materiaPrimaSede.setCantidadActualMl(50);
        materiaPrimaSede.setActiva(true);

        when(inventarioRepository.findProductosSueltosBySedeIdOrderByProductoCodigoAsc(3L))
                .thenReturn(List.of(inventario));
        when(materiaPrimaSedeRepository.findActivasBySedeIdOrderByMateriaPrimaNombreAsc(3L))
                .thenReturn(List.of(materiaPrimaSede));

        var respuesta = inventarioServicio.listarInventarioAjustablePorSede(3L);

        assertEquals(3L, respuesta.sedeId());
        assertEquals(2, respuesta.items().size());
        assertEquals(TipoInventarioAjustable.PRODUCTO, respuesta.items().get(0).tipo());
        assertEquals("Gaseosa 400ml", respuesta.items().get(0).nombre());
        assertEquals(12.0, respuesta.items().get(0).stockActual());
        assertEquals(TipoInventarioAjustable.MATERIA_PRIMA, respuesta.items().get(1).tipo());
        assertEquals("Harina", respuesta.items().get(1).nombre());
        assertEquals(50.0, respuesta.items().get(1).stockActual());
    }

    @Test
    void ajustarInventarioManualDebeActualizarProductoYMateriaPrima() {
        Empresa empresa = new Empresa();
        empresa.setNit(1007960474L);

        Producto producto = new Producto();
        producto.setCodigo(1L);
        producto.setNombre("Gaseosa 400ml");
        producto.setEmpresa(empresa);

        Sede sede = new Sede();
        sede.setId(3L);
        sede.setEmpresa(empresa);

        Inventario inventario = new Inventario();
        inventario.setProducto(producto);
        inventario.setSede(sede);
        inventario.setStockActual(12);

        MateriaPrima materiaPrima = new MateriaPrima();
        materiaPrima.setCodigo(8L);
        materiaPrima.setNombre("Harina");
        materiaPrima.setActiva(true);

        MateriaPrimaSede materiaPrimaSede = new MateriaPrimaSede();
        materiaPrimaSede.setMateriaPrima(materiaPrima);
        materiaPrimaSede.setSede(sede);
        materiaPrimaSede.setCantidadActualMl(50);
        materiaPrimaSede.setActiva(true);

        when(inventarioRepository.findVisibleByProductoCodigoAndSedeId(1L, 3L)).thenReturn(Optional.of(inventario));
        when(inventarioRepository.save(any(Inventario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(materiaPrimaSedeRepository.findByMateriaPrimaCodigoAndSedeId(8L, 3L)).thenReturn(Optional.of(materiaPrimaSede));
        when(materiaPrimaSedeRepository.save(any(MateriaPrimaSede.class))).thenAnswer(inv -> inv.getArgument(0));

        var respuesta = inventarioServicio.ajustarInventarioManual(
                3L,
                List.of(
                        new AjusteManualItemDTO(1L, TipoInventarioAjustable.PRODUCTO, 20.0),
                        new AjusteManualItemDTO(8L, TipoInventarioAjustable.MATERIA_PRIMA, 45.0)
                )
        );

        assertEquals(3L, respuesta.sedeId());
        assertEquals(2, respuesta.actualizados());
        assertEquals(20, inventario.getStockActual());
        assertEquals(45.0, materiaPrimaSede.getCantidadActualMl());
        assertEquals(12.0, respuesta.resultado().get(0).stockAnterior());
        assertEquals(20.0, respuesta.resultado().get(0).stockNuevo());
        assertEquals(50.0, respuesta.resultado().get(1).stockAnterior());
        assertEquals(45.0, respuesta.resultado().get(1).stockNuevo());
    }

    @Test
    void ajustarInventarioManualDebeRechazarProductoConReceta() {
        Empresa empresa = new Empresa();
        empresa.setNit(1007960474L);

        Producto producto = new Producto();
        producto.setCodigo(1L);
        producto.setNombre("Malteada");
        producto.setEmpresa(empresa);
        producto.setMateriasPrimas(List.of(new ProductoMateriaPrima()));

        Sede sede = new Sede();
        sede.setId(3L);
        sede.setEmpresa(empresa);

        Inventario inventario = new Inventario();
        inventario.setProducto(producto);
        inventario.setSede(sede);
        inventario.setStockActual(12);

        when(inventarioRepository.findVisibleByProductoCodigoAndSedeId(1L, 3L)).thenReturn(Optional.of(inventario));

        RuntimeException error = assertThrows(RuntimeException.class, () ->
                inventarioServicio.ajustarInventarioManual(
                        3L,
                        List.of(new AjusteManualItemDTO(1L, TipoInventarioAjustable.PRODUCTO, 20.0))
                ));

        assertEquals("No se puede ajustar manualmente un producto con receta", error.getMessage());
    }
}
