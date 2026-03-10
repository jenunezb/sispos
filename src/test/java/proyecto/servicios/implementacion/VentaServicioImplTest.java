package proyecto.servicios.implementacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import proyecto.entidades.Empresa;
import proyecto.entidades.Sede;
import proyecto.entidades.Venta;
import proyecto.repositorios.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VentaServicioImplTest {

    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private VendedorRepository vendedorRepository;
    @Mock
    private SedeRepository sedeRepository;
    @Mock
    private MateriaPrimaSedeRepository materiaPrimaSedeRepository;
    @Mock
    private MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock
    private InventarioRepository inventarioRepository;
    @Mock
    private AdministradorRepository administradorRepository;

    @InjectMocks
    private VentaServicioImpl ventaServicio;

    @Test
    void cambiarEstadoVentaDebeMarcarInvalidaYValidaPorEmpresa() {
        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);

        Sede sede = new Sede();
        sede.setEmpresa(empresa);

        Venta venta = new Venta();
        venta.setId(10L);
        venta.setSede(sede);
        venta.setAnulado(false);

        when(ventaRepository.findByIdAndSedeEmpresaNit(10L, 900123456L))
                .thenReturn(Optional.of(venta));

        ventaServicio.cambiarEstadoVenta(10L, false, 900123456L);
        assertTrue(venta.getAnulado());

        ventaServicio.cambiarEstadoVenta(10L, true, 900123456L);
        assertFalse(venta.getAnulado());

        verify(ventaRepository, times(2)).save(venta);
    }
}
