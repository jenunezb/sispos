package proyecto.servicios.implementacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import proyecto.dto.ActualizarConsumoProductoDTO;
import proyecto.dto.CrearMateriaPrimaSedeDTO;
import proyecto.dto.CargaMateriaPrimaItemDTO;
import proyecto.dto.CargaMateriaPrimaMasivaDTO;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);

        MateriaPrima materiaPrima = new MateriaPrima();
        materiaPrima.setCodigo(1L);
        materiaPrima.setNombre("Queso");

        Sede sede = new Sede();
        sede.setId(5L);
        sede.setEmpresa(empresa);

        when(materiaPrimaRepository.findByNombreIgnoreCaseAndEmpresaNit("Queso", empresa.getNit()))
                .thenReturn(Optional.of(materiaPrima));
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
        relacion.setMlConsumidos(80);

        when(materiaPrimaSedeRepository.findById(4L)).thenReturn(Optional.of(mpSede));
        when(inventarioRepository.findVisibleByProductoCodigoAndSedeId(99L, 7L)).thenReturn(Optional.of(inventario));
        when(productoMateriaPrimaRepository.findByMateriaPrimaCodigoAndProductoCodigo(10L, 99L))
                .thenReturn(Optional.of(relacion));

        materiaPrimaSedeService.actualizarConsumoProducto(4L, 99L, new ActualizarConsumoProductoDTO(125));

        assertEquals(125, relacion.getMlConsumidos());
        verify(productoMateriaPrimaRepository).save(relacion);
    }

    @Test
    void cargaMasivaDebeAlimentarExistenteYCalcularCostoPromedio() {
        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);
        Sede sede = new Sede();
        sede.setId(5L);
        sede.setEmpresa(empresa);

        MateriaPrima materia = new MateriaPrima();
        materia.setCodigo(10L);
        materia.setNombre("Cucharas");
        materia.setUnidadBase("UNIDAD");
        materia.setCostoUnitario(50D);

        MateriaPrimaSede materiaSede = new MateriaPrimaSede();
        materiaSede.setMateriaPrima(materia);
        materiaSede.setSede(sede);
        materiaSede.setCantidadActualMl(200D);

        when(sedeRepository.findById(5L)).thenReturn(Optional.of(sede));
        when(materiaPrimaRepository.findByNombreIgnoreCaseAndEmpresaNit("Cucharas", empresa.getNit()))
                .thenReturn(Optional.of(materia));
        when(materiaPrimaSedeRepository.sumarStockMateriaPrima(10L)).thenReturn(200D);
        when(materiaPrimaSedeRepository.findByMateriaPrimaCodigoAndSedeId(10L, 5L))
                .thenReturn(Optional.of(materiaSede));

        var resultado = materiaPrimaSedeService.cargarMasivamente(new CargaMateriaPrimaMasivaDTO(
                5L,
                List.of(new CargaMateriaPrimaItemDTO("Cucharas", "UNIDAD", "Paquete", 2, 100, 7000))
        ));

        assertEquals(400D, materiaSede.getCantidadActualMl());
        assertEquals(60D, materia.getCostoUnitario());
        assertEquals(1, resultado.size());
        assertEquals(false, resultado.get(0).creada());
    }
}
