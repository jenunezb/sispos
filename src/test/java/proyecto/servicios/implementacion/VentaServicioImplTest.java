package proyecto.servicios.implementacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import proyecto.dto.DetalleVentaDTO;
import proyecto.dto.VentaRecuestDTO;
import proyecto.dto.VentaResponseDTO;
import proyecto.entidades.*;
import proyecto.repositorios.AdministradorRepository;
import proyecto.repositorios.ClienteRepository;
import proyecto.repositorios.InventarioProduccionRepository;
import proyecto.repositorios.InventarioRepository;
import proyecto.repositorios.MateriaPrimaSedeRepository;
import proyecto.repositorios.MovimientoInventarioRepository;
import proyecto.repositorios.MovimientoProduccionRepository;
import proyecto.repositorios.PrecioClienteProductoRepository;
import proyecto.repositorios.ProductoRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.repositorios.VendedorRepository;
import proyecto.repositorios.VentaRepository;
import proyecto.servicios.interfaces.MesaEstadoServicio;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class VentaServicioImplTest {

    @Mock private VentaRepository ventaRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private VendedorRepository vendedorRepository;
    @Mock private SedeRepository sedeRepository;
    @Mock private MateriaPrimaSedeRepository materiaPrimaSedeRepository;
    @Mock private MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock private InventarioRepository inventarioRepository;
    @Mock private AdministradorRepository administradorRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private PrecioClienteProductoRepository precioClienteProductoRepository;
    @Mock private InventarioProduccionRepository inventarioProduccionRepository;
    @Mock private MovimientoProduccionRepository movimientoProduccionRepository;
    @Mock private NotificacionStockMinimoService notificacionStockMinimoService;
    @Mock private MesaEstadoServicio mesaEstadoServicio;

    @InjectMocks
    private VentaServicioImpl ventaServicio;

    @Test
    void rechazaClienteAjenoAntesDeGuardarVentaOModificarInventario() {
        prepararVentaConCliente();
        when(clienteRepository.findByIdAndEmpresaNit(7L, 123L)).thenReturn(Optional.empty());
        var error = assertThrows(RuntimeException.class, () -> ventaServicio.crearVenta(ventaConCliente(7L)));
        assertTrue(error.getMessage().contains("empresa de la venta"));
        verify(ventaRepository, never()).save(any());
        verifyNoInteractions(productoRepository, inventarioRepository, movimientoInventarioRepository);
    }

    @Test
    void guardaClientePropioYDevuelveSusDatosCompletos() {
        Empresa empresa = prepararVentaConCliente();
        Cliente cliente = new Cliente();
        cliente.setId(7L);
        cliente.setEmpresa(empresa);
        cliente.setNombre("Cliente A");
        cliente.setDocumento("900-1");
        cliente.setCorreo("cliente@ejemplo.com");
        cliente.setTelefono("3001234567");
        cliente.setDireccion("Calle 1");
        when(clienteRepository.findByIdAndEmpresaNit(7L, 123L)).thenReturn(Optional.of(cliente));
        when(ventaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var respuesta = ventaServicio.mapToResponse(ventaServicio.crearVenta(ventaConCliente(7L)));
        assertEquals(7L, respuesta.clienteId());
        assertEquals("900-1", respuesta.cliente().documento());
        assertEquals("cliente@ejemplo.com", respuesta.cliente().correo());
        assertEquals("3001234567", respuesta.cliente().telefono());
        assertEquals("Calle 1", respuesta.cliente().direccion());
    }

    @Test
    void rechazaClienteInactivo() {
        prepararVentaConCliente();
        Cliente cliente = new Cliente();
        cliente.setActivo(false);
        when(clienteRepository.findByIdAndEmpresaNit(7L, 123L)).thenReturn(Optional.of(cliente));
        assertThrows(RuntimeException.class, () -> ventaServicio.crearVenta(ventaConCliente(7L)));
        verify(ventaRepository, never()).save(any());
    }

    @Test
    void consumidorFinalNoConsultaClientes() {
        prepararVentaConCliente();
        when(ventaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var respuesta = ventaServicio.mapToResponse(ventaServicio.crearVenta(ventaConCliente(null)));
        org.junit.jupiter.api.Assertions.assertNull(respuesta.cliente());
        verifyNoInteractions(clienteRepository);
    }

    @Test
    void ventaDomicilioConservaDatosYSumaCostoAlTotal() {
        prepararVentaConCliente();
        when(ventaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        VentaRecuestDTO dto = new VentaRecuestDTO(
                "vendedor@ejemplo.com", 22L, null,
                List.of(new DetalleVentaDTO(null, "Hamburguesa", 12000.0, 1)),
                ModoPago.EFECTIVO, 15000.0, 15000.0, 0.0, 9991L,
                true, "Calle 10 # 5-20", 3000.0, "Laura", "3001234567"
        );

        VentaResponseDTO respuesta = ventaServicio.mapToResponse(ventaServicio.crearVenta(dto));

        assertTrue(respuesta.esDomicilio());
        assertEquals("Calle 10 # 5-20", respuesta.direccionDomicilio());
        assertEquals(3000.0, respuesta.costoDomicilio());
        assertEquals("Laura", respuesta.nombreRecibeDomicilio());
        assertEquals("3001234567", respuesta.celularRecibeDomicilio());
        assertEquals(15000.0, respuesta.total());
    }

    private Empresa prepararVentaConCliente() {
        Empresa empresa = new Empresa();
        empresa.setNit(123L);
        Sede sede = new Sede();
        sede.setId(22L);
        sede.setEmpresa(empresa);
        Vendedor vendedor = new Vendedor();
        vendedor.setCorreo("vendedor@ejemplo.com");
        vendedor.setEmpresa(empresa);
        vendedor.setSede(sede);
        when(vendedorRepository.findByCorreoIgnoreCase("vendedor@ejemplo.com")).thenReturn(Optional.of(vendedor));
        when(sedeRepository.findByIdForUpdate(22L)).thenReturn(Optional.of(sede));
        return empresa;
    }

    private VentaRecuestDTO ventaConCliente(Long clienteId) {
        return new VentaRecuestDTO("vendedor@ejemplo.com", 22L, clienteId,
                List.of(new DetalleVentaDTO(null, "Producto libre", 1000.0, 1)),
                ModoPago.EFECTIVO, null, null, null);
    }

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

    @Test
    void crearVentaProduccionDebeUsarSedeDelVendedorAunqueRequestTengaOtra() {
        Sede sedeProduccion = new Sede();
        sedeProduccion.setId(10L);

        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);

        Vendedor vendedor = new Vendedor();
        vendedor.setCorreo("prod@correo.com");
        vendedor.setTipoPerfil(TipoPerfilVendedor.PRODUCCION);
        vendedor.setSede(sedeProduccion);
        vendedor.setEmpresa(empresa);
        sedeProduccion.setEmpresa(empresa);

        Cliente cliente = new Cliente();
        cliente.setId(7L);
        cliente.setEmpresa(empresa);
        cliente.setActivo(true);

        Producto producto = new Producto();
        producto.setCodigo(99L);
        producto.setPrecioVenta(12000.0);

        InventarioProduccion inventario = new InventarioProduccion();
        inventario.setProducto(producto);
        inventario.setSede(sedeProduccion);
        inventario.setStockActual(50);

        VentaRecuestDTO dto = new VentaRecuestDTO(
                "cualquier@valor.com",
                999L,
                7L,
                List.of(new DetalleVentaDTO(99L, null, null, 2)),
                ModoPago.EFECTIVO,
                null,
                null,
                null
        );

        when(vendedorRepository.findByCorreoIgnoreCase("prod@correo.com")).thenReturn(Optional.of(vendedor));
        when(clienteRepository.findByIdAndEmpresaNit(7L, empresa.getNit())).thenReturn(Optional.of(cliente));
        when(productoRepository.findById(99L)).thenReturn(Optional.of(producto));
        when(sedeRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(sedeProduccion));
        when(inventarioProduccionRepository.findByProductoCodigoAndSedeId(99L, 10L)).thenReturn(Optional.of(inventario));
        when(precioClienteProductoRepository.findByClienteIdAndProductoCodigo(7L, 99L)).thenReturn(Optional.empty());
        when(ventaRepository.findMaxNumeroConsecutivoBySedeId(10L)).thenReturn(0L);
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));

        ventaServicio.crearVentaProduccion("prod@correo.com", dto);

        ArgumentCaptor<Venta> ventaCaptor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepository).save(ventaCaptor.capture());
        assertEquals(10L, ventaCaptor.getValue().getSede().getId());
        assertEquals(1L, ventaCaptor.getValue().getNumeroConsecutivo());
    }

    @Test
    void crearVentaVendedorDebeUsarSedeDelRequest() {
        Sede sedeRequest = new Sede();
        sedeRequest.setId(22L);

        Vendedor vendedor = new Vendedor();
        vendedor.setCorreo("vend@correo.com");
        vendedor.setTipoPerfil(TipoPerfilVendedor.VENDEDOR);

        Producto producto = new Producto();
        producto.setCodigo(40L);
        producto.setPrecioVenta(8000.0);

        Inventario inventario = new Inventario();
        inventario.setProducto(producto);
        inventario.setSede(sedeRequest);
        inventario.setStockActual(30);

        VentaRecuestDTO dto = new VentaRecuestDTO(
                "vend@correo.com",
                22L,
                null,
                List.of(new DetalleVentaDTO(40L, null, null, 1)),
                ModoPago.EFECTIVO,
                null,
                null,
                null
        );

        when(vendedorRepository.findByCorreoIgnoreCase("vend@correo.com")).thenReturn(Optional.of(vendedor));
        when(sedeRepository.findByIdForUpdate(22L)).thenReturn(Optional.of(sedeRequest));
        when(productoRepository.findById(40L)).thenReturn(Optional.of(producto));
        when(inventarioRepository.findVisibleByProductoCodigoAndSedeId(40L, 22L)).thenReturn(Optional.of(inventario));
        when(ventaRepository.findMaxNumeroConsecutivoBySedeId(22L)).thenReturn(3L);
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));

        ventaServicio.crearVenta(dto);

        ArgumentCaptor<Venta> ventaCaptor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepository).save(ventaCaptor.capture());
        assertEquals(22L, ventaCaptor.getValue().getSede().getId());
        assertEquals(4L, ventaCaptor.getValue().getNumeroConsecutivo());
    }

    @Test
    void obtenerVentaPorIdDebeIncluirModoPagoEnLaRespuesta() {
        Sede sede = new Sede();
        sede.setUbicacion("Centro");

        Venta venta = new Venta();
        venta.setId(15L);
        venta.setNumeroConsecutivo(8L);
        venta.setFecha(java.time.LocalDateTime.of(2026, 3, 29, 18, 30));
        venta.setTotal(25000.0);
        venta.setModoPago(ModoPago.TRANSFERENCIA);
        venta.setMontoEfectivo(null);
        venta.setMontoTransferencia(null);
        venta.setSede(sede);
        venta.setDetalles(List.of());
        venta.setAnulado(false);

        when(ventaRepository.findDetalleById(15L)).thenReturn(Optional.of(venta));

        var respuesta = ventaServicio.obtenerVentaPorId(15L);

        assertEquals(15L, respuesta.id());
        assertEquals(8L, respuesta.consecutivo());
        assertEquals("TRANSFERENCIA", respuesta.modoPago());
        assertEquals(0.0, respuesta.montoEfectivo());
        assertEquals(25000.0, respuesta.montoTransferencia());
        assertEquals("Centro", respuesta.sedeUbicacion());
    }

    @Test
    void crearVentaDebeCalcularConsecutivoIndependientePorSede() {
        Sede sede = new Sede();
        sede.setId(55L);

        Vendedor vendedor = new Vendedor();
        vendedor.setCorreo("sede55@correo.com");
        vendedor.setTipoPerfil(TipoPerfilVendedor.VENDEDOR);

        Producto producto = new Producto();
        producto.setCodigo(11L);
        producto.setPrecioVenta(5000.0);

        Inventario inventario = new Inventario();
        inventario.setProducto(producto);
        inventario.setSede(sede);
        inventario.setStockActual(10);

        VentaRecuestDTO dto = new VentaRecuestDTO(
                "sede55@correo.com",
                55L,
                null,
                List.of(new DetalleVentaDTO(11L, null, null, 1)),
                ModoPago.EFECTIVO,
                null,
                null,
                null
        );

        when(vendedorRepository.findByCorreoIgnoreCase("sede55@correo.com")).thenReturn(Optional.of(vendedor));
        when(sedeRepository.findByIdForUpdate(55L)).thenReturn(Optional.of(sede));
        when(productoRepository.findById(11L)).thenReturn(Optional.of(producto));
        when(inventarioRepository.findVisibleByProductoCodigoAndSedeId(11L, 55L)).thenReturn(Optional.of(inventario));
        when(ventaRepository.findMaxNumeroConsecutivoBySedeId(eq(55L))).thenReturn(7L);
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));

        ventaServicio.crearVenta(dto);

        ArgumentCaptor<Venta> ventaCaptor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepository).save(ventaCaptor.capture());
        assertEquals(8L, ventaCaptor.getValue().getNumeroConsecutivo());
    }

    @Test
    void crearVentaMixtaDebeGuardarDesgloseDeMontos() {
        Sede sede = new Sede();
        sede.setId(66L);

        Vendedor vendedor = new Vendedor();
        vendedor.setCorreo("mixto@correo.com");
        vendedor.setTipoPerfil(TipoPerfilVendedor.VENDEDOR);

        Producto producto = new Producto();
        producto.setCodigo(12L);
        producto.setPrecioVenta(10000.0);

        Inventario inventario = new Inventario();
        inventario.setProducto(producto);
        inventario.setSede(sede);
        inventario.setStockActual(10);

        VentaRecuestDTO dto = new VentaRecuestDTO(
                "mixto@correo.com",
                66L,
                null,
                List.of(new DetalleVentaDTO(12L, null, null, 1)),
                ModoPago.MIXTO,
                null,
                4000.0,
                6000.0
        );

        when(vendedorRepository.findByCorreoIgnoreCase("mixto@correo.com")).thenReturn(Optional.of(vendedor));
        when(sedeRepository.findByIdForUpdate(66L)).thenReturn(Optional.of(sede));
        when(productoRepository.findById(12L)).thenReturn(Optional.of(producto));
        when(inventarioRepository.findVisibleByProductoCodigoAndSedeId(12L, 66L)).thenReturn(Optional.of(inventario));
        when(ventaRepository.findMaxNumeroConsecutivoBySedeId(66L)).thenReturn(0L);
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));

        ventaServicio.crearVenta(dto);

        ArgumentCaptor<Venta> ventaCaptor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepository).save(ventaCaptor.capture());
        assertEquals(ModoPago.MIXTO, ventaCaptor.getValue().getModoPago());
        assertEquals(4000.0, ventaCaptor.getValue().getMontoEfectivo());
        assertEquals(6000.0, ventaCaptor.getValue().getMontoTransferencia());
    }

    @Test
    void crearVentaMixtaDebeFallarSiLaSumaNoCoincideConElTotal() {
        Sede sede = new Sede();
        sede.setId(77L);

        Vendedor vendedor = new Vendedor();
        vendedor.setCorreo("error@correo.com");
        vendedor.setTipoPerfil(TipoPerfilVendedor.VENDEDOR);

        Producto producto = new Producto();
        producto.setCodigo(13L);
        producto.setPrecioVenta(10000.0);

        Inventario inventario = new Inventario();
        inventario.setProducto(producto);
        inventario.setSede(sede);
        inventario.setStockActual(10);

        VentaRecuestDTO dto = new VentaRecuestDTO(
                "error@correo.com",
                77L,
                null,
                List.of(new DetalleVentaDTO(13L, null, null, 1)),
                ModoPago.MIXTO,
                null,
                3000.0,
                6000.0
        );

        when(vendedorRepository.findByCorreoIgnoreCase("error@correo.com")).thenReturn(Optional.of(vendedor));
        when(sedeRepository.findByIdForUpdate(77L)).thenReturn(Optional.of(sede));
        when(productoRepository.findById(13L)).thenReturn(Optional.of(producto));
        when(inventarioRepository.findVisibleByProductoCodigoAndSedeId(13L, 77L)).thenReturn(Optional.of(inventario));
        when(ventaRepository.findMaxNumeroConsecutivoBySedeId(77L)).thenReturn(0L);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> ventaServicio.crearVenta(dto));
        assertEquals("La suma de efectivo y transferencia debe ser igual al total", exception.getMessage());
    }
}
