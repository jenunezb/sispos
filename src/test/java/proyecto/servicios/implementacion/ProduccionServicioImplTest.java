package proyecto.servicios.implementacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import proyecto.dto.ClienteCrearDTO;
import proyecto.dto.PrecioClienteRequestDTO;
import proyecto.entidades.*;
import proyecto.repositorios.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProduccionServicioImplTest {

    @Mock
    private VendedorRepository vendedorRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private PrecioClienteProductoRepository precioClienteProductoRepository;

    @InjectMocks
    private ProduccionServicioImpl produccionServicio;

    @Test
    void crearClienteDebeAsociarseAEmpresaDeProduccion() {
        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);

        Vendedor produccion = new Vendedor();
        produccion.setCorreo("prod@correo.com");
        produccion.setTipoPerfil(TipoPerfilVendedor.PRODUCCION);
        produccion.setEmpresa(empresa);

        when(vendedorRepository.findByCorreo("prod@correo.com")).thenReturn(Optional.of(produccion));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        var dto = new ClienteCrearDTO("Cliente A", "3001112233", "CC123");
        var creado = produccionServicio.crearCliente("prod@correo.com", dto);

        assertEquals(1L, creado.id());
        assertEquals("Cliente A", creado.nombre());
    }

    @Test
    void guardarPrecioClienteDebePersistirPrecio() {
        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);

        Vendedor produccion = new Vendedor();
        produccion.setCorreo("prod@correo.com");
        produccion.setTipoPerfil(TipoPerfilVendedor.PRODUCCION);
        produccion.setEmpresa(empresa);

        Cliente cliente = new Cliente();
        cliente.setId(10L);
        cliente.setEmpresa(empresa);

        Producto producto = new Producto();
        producto.setCodigo(20L);
        producto.setNombre("Producto X");
        producto.setEmpresa(empresa);

        when(vendedorRepository.findByCorreo("prod@correo.com")).thenReturn(Optional.of(produccion));
        when(clienteRepository.findByIdAndEmpresaNit(10L, 900123456L)).thenReturn(Optional.of(cliente));
        when(productoRepository.findById(20L)).thenReturn(Optional.of(producto));
        when(precioClienteProductoRepository.findByClienteIdAndProductoCodigo(10L, 20L)).thenReturn(Optional.empty());
        when(precioClienteProductoRepository.save(any(PrecioClienteProducto.class))).thenAnswer(inv -> {
            PrecioClienteProducto p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });

        var respuesta = produccionServicio.guardarPrecioCliente(
                "prod@correo.com",
                10L,
                new PrecioClienteRequestDTO(20L, 15500.0)
        );

        assertEquals(100L, respuesta.id());
        assertEquals(15500.0, respuesta.precioVenta());
    }
}
