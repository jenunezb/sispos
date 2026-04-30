package proyecto.servicios.implementacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import proyecto.dto.ActualizarConsumoProductoDTO;
import proyecto.dto.CrearMateriaPrimaSedeDTO;
import proyecto.dto.VincularProductoDTO;
import proyecto.entidades.Empresa;
import proyecto.entidades.Inventario;
import proyecto.entidades.MateriaPrima;
import proyecto.entidades.MateriaPrimaSede;
import proyecto.entidades.Producto;
import proyecto.entidades.ProductoMateriaPrima;
import proyecto.entidades.Sede;
import proyecto.repositorios.InventarioRepository;
import proyecto.repositorios.MateriaPrimaRepository;
import proyecto.repositorios.MateriaPrimaSedeRepository;
import proyecto.repositorios.ProductoMateriaPrimaRepository;
import proyecto.repositorios.ProductoRepository;
import proyecto.repositorios.SedeRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MateriaPrimaSedeServiceImplTest {

    @Mock private MateriaPrimaSedeRepository materiaPrimaSedeRepository;
    @Mock private MateriaPrimaRepository materiaPrimaRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private ProductoMateriaPrimaRepository productoMateriaPrimaRepository;
    @Mock private SedeRepository sedeRepository;
    @Mock private InventarioRepository inventarioRepository;

    @InjectMocks
    private MateriaPrimaSedeServiceImpl materiaPrimaSedeService;

    @Test
    void crearYVincularDebeGuardarMlPorVasoEnCeroPorqueElConsumoEsPorProducto() {
        MateriaPrima materiaPrima = new MateriaPrima();
        materiaPrima.setCodigo(1L);
        materiaPrima.setNombre("Queso");

        Sede sede = new Sede();
        sede.setId(5L);

        when(materiaPrimaRepository.findByNombreIgnoreCase("Queso")).thenReturn(Optional.of(materiaPrima));
        when(materiaPrimaSedeRepository.existsByMateriaPrimaAndSedeId(materiaPrima, 5L)).thenReturn(false);
        when(sedeRepository.findById(5L)).thenReturn(Optional.of(sede));

        materiaPrimaSedeService.crearYVincular(new CrearMateriaPrimaSedeDTO("Queso", true, 5L, 3000, 180));

        ArgumentCaptor<MateriaPrimaSede> captor = ArgumentCaptor.forClass(MateriaPrimaSede.class);
        verify(materiaPrimaSedeRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getMlPorVaso());
    }

    @Test
    void actualizarConsumoProductoDebeGuardarMlConsumidosEnLaRelacion() {
        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);

        Sede sede = new Sede();
        sede.setId(7L);
        sede.setEmpresa(empresa);

        MateriaPrima materiaPrima = new MateriaPrima();
        materiaPrima.setCodigo(10L);

        MateriaPrimaSede mpSede = new MateriaPrimaSede();
        mpSede.setId(4L);
        mpSede.setMateriaPrima(materiaPrima);
        mpSede.setSede(sede);

        Producto producto = new Producto();
        producto.setCodigo(99L);

        Inventario inventario = new Inventario();
        inventario.setProducto(producto);
        inventario.setSede(sede);

        ProductoMateriaPrima relacion = new ProductoMateriaPrima();
        relacion.setProducto(producto);
        relacion.setMateriaPrima(materiaPrima);
        relacion.setMateriaPrimaSede(mpSede);
        relacion.setMlConsumidos(80);

        when(materiaPrimaSedeRepository.findById(4L)).thenReturn(Optional.of(mpSede));
        when(inventarioRepository.findVisibleByProductoCodigoAndSedeId(99L, 7L)).thenReturn(Optional.of(inventario));
        when(productoMateriaPrimaRepository.findByMateriaPrimaSedeIdAndProductoCodigo(4L, 99L))
                .thenReturn(Optional.of(relacion));

        materiaPrimaSedeService.actualizarConsumoProducto(4L, 99L, new ActualizarConsumoProductoDTO(125));

        assertEquals(125, relacion.getMlConsumidos());
        verify(productoMateriaPrimaRepository).save(relacion);
    }

    @Test
    void vincularProductoDebePermitirMismoProductoYMateriaPrimaEnOtraSede() {
        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);

        Sede sede = new Sede();
        sede.setId(8L);
        sede.setEmpresa(empresa);

        MateriaPrima materiaPrima = new MateriaPrima();
        materiaPrima.setCodigo(20L);

        MateriaPrimaSede mpSede = new MateriaPrimaSede();
        mpSede.setId(15L);
        mpSede.setSede(sede);
        mpSede.setMateriaPrima(materiaPrima);

        Producto producto = new Producto();
        producto.setCodigo(30L);

        Inventario inventario = new Inventario();
        inventario.setProducto(producto);
        inventario.setSede(sede);

        when(materiaPrimaSedeRepository.findById(15L)).thenReturn(Optional.of(mpSede));
        when(productoRepository.findById(30L)).thenReturn(Optional.of(producto));
        when(inventarioRepository.findVisibleByProductoCodigoAndSedeId(30L, 8L)).thenReturn(Optional.of(inventario));
        when(productoMateriaPrimaRepository.existsByMateriaPrimaSedeIdAndProductoCodigo(15L, 30L)).thenReturn(false);

        assertDoesNotThrow(() -> materiaPrimaSedeService.vincularProducto(new VincularProductoDTO(15L, 30L, 90)));
        verify(productoMateriaPrimaRepository).save(org.mockito.ArgumentMatchers.any(ProductoMateriaPrima.class));
    }
}
