package proyecto.servicios.implementacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import proyecto.entidades.Empresa;
import proyecto.entidades.Inventario;
import proyecto.entidades.Producto;
import proyecto.entidades.Sede;
import proyecto.repositorios.InventarioRepository;
import proyecto.repositorios.ProductoRepository;
import proyecto.repositorios.SedeRepository;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServicioImplTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private InventarioRepository inventarioRepository;

    @Mock
    private SedeRepository sedeRepository;

    @InjectMocks
    private ProductoServicioImpl productoServicio;

    @Test
    void importarProductosCsvDebeCrearProductosParaEmpresaDelAdmin() {
        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);

        Sede sede = new Sede();
        sede.setId(1L);
        sede.setEmpresa(empresa);

        when(sedeRepository.findByEmpresaNit(900123456L)).thenReturn(List.of(sede));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> {
            Producto p = invocation.getArgument(0);
            p.setCodigo(100L);
            return p;
        });

        String csv = "Codigo,\"Nombre\",\"Descripcion\",\"Precio Produccion\",\"Precio Venta\",\"Estado\"\n" +
                "1,\"Omelette\",\"Desc\",\"0\",\"12000\",\"Activo\"\n";

        MockMultipartFile archivo = new MockMultipartFile(
                "file",
                "productos.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        int total = productoServicio.importarProductosCsv(archivo, 900123456L);

        assertEquals(1, total);

        ArgumentCaptor<Producto> captorProducto = ArgumentCaptor.forClass(Producto.class);
        verify(productoRepository).save(captorProducto.capture());
        assertEquals("Omelette", captorProducto.getValue().getNombre());
        assertEquals(empresa, captorProducto.getValue().getEmpresa());

        verify(inventarioRepository).save(any(Inventario.class));
    }

    @Test
    void listarProductosDebeFiltrarPorSedeCuandoSeIndica() {
        Empresa empresa = new Empresa();
        empresa.setNit(1097726190L);

        Producto producto = new Producto();
        producto.setCodigo(235L);
        producto.setNombre("Pastel de pollo");
        producto.setDescripcion("Desc");
        producto.setPrecioProduccion(2450D);
        producto.setPrecioVenta(5500D);
        producto.setEstado(true);
        producto.setEmpresa(empresa);

        when(productoRepository.findActivosByEmpresaNitAndSedeIdOrderByCodigoAsc(1097726190L, 7L))
                .thenReturn(List.of(producto));

        var respuesta = productoServicio.listarProductos(1097726190L, 7L);

        assertEquals(1, respuesta.size());
        assertEquals(235L, respuesta.get(0).codigo());
        verify(productoRepository).findActivosByEmpresaNitAndSedeIdOrderByCodigoAsc(eq(1097726190L), eq(7L));
    }

    @Test
    void crearConCodigoDeBarrasConservaCerosInicialesYLoExpone() {
        Empresa empresa = new Empresa(); empresa.setNit(123L);
        Sede sede = new Sede(); sede.setEmpresa(empresa);
        when(sedeRepository.findById(1L)).thenReturn(java.util.Optional.of(sede));
        when(productoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var dto = new proyecto.dto.ProductoCrearDTO("Cafe", "Desc", 0D, 2000D, null, 1L, " 0012345 ");
        var creado = productoServicio.crearProducto(dto);
        assertEquals("0012345", creado.codigoBarras());
        verify(productoRepository).findByEmpresaNitAndCodigoBarras(123L, "0012345");
    }

    @Test
    void noPermiteCodigoDuplicadoEnLaEmpresa() {
        Empresa empresa = new Empresa(); empresa.setNit(123L);
        Sede sede = new Sede(); sede.setEmpresa(empresa);
        Producto existente = new Producto(); existente.setCodigo(2L);
        when(sedeRepository.findById(1L)).thenReturn(java.util.Optional.of(sede));
        when(productoRepository.findByEmpresaNitAndCodigoBarras(123L, "00123"))
                .thenReturn(java.util.Optional.of(existente));
        var dto = new proyecto.dto.ProductoCrearDTO("Cafe", "Desc", 0D, 2000D, null, 1L, "00123");
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> productoServicio.crearProducto(dto));
        verify(productoRepository, never()).save(any());
        verifyNoInteractions(inventarioRepository);
    }

    @Test
    void actualizarPermiteConservarYEliminarCodigoPropio() {
        Empresa empresa = new Empresa(); empresa.setNit(123L);
        Producto producto = new Producto(); producto.setCodigo(2L); producto.setEmpresa(empresa);
        when(productoRepository.findById(2L)).thenReturn(java.util.Optional.of(producto));
        when(productoRepository.findByEmpresaNitAndCodigoBarras(123L, "00123"))
                .thenReturn(java.util.Optional.of(producto));
        when(productoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var dto = new proyecto.dto.ProductoActualizarDTO(2L, "Cafe", "Desc", 0D, 2000D, null, true, "00123");
        assertEquals("00123", productoServicio.actualizarProducto(dto).codigoBarras());
        var sinCodigo = new proyecto.dto.ProductoActualizarDTO(2L, "Cafe", "Desc", 0D, 2000D, null, true, " ");
        org.junit.jupiter.api.Assertions.assertNull(productoServicio.actualizarProducto(sinCodigo).codigoBarras());
    }

    @Test
    void rechazaCodigoDemasiadoLargo() {
        Empresa empresa = new Empresa(); empresa.setNit(123L);
        Sede sede = new Sede(); sede.setEmpresa(empresa);
        when(sedeRepository.findById(1L)).thenReturn(java.util.Optional.of(sede));
        var dto = new proyecto.dto.ProductoCrearDTO("Cafe", "Desc", 0D, 2000D, null, 1L, "1".repeat(101));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> productoServicio.crearProducto(dto));
        verify(productoRepository, never()).save(any());
    }
}
