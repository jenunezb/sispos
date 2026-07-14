package proyecto.servicios.implementacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import proyecto.dto.ClienteActualizarDTO;
import proyecto.dto.ClienteCrearDTO;
import proyecto.dto.PrecioClienteRequestDTO;
import proyecto.dto.ProduccionAjusteManualRequestDTO;
import proyecto.dto.ProduccionAjusteManualRequestItemDTO;
import proyecto.dto.ProduccionRegistroItemDTO;
import proyecto.dto.ProduccionRegistroMultipleDTO;
import proyecto.dto.ProductoProduccionRequestDTO;
import proyecto.entidades.Cliente;
import proyecto.entidades.Empresa;
import proyecto.entidades.InventarioProduccion;
import proyecto.entidades.MovimientoProduccion;
import proyecto.entidades.PrecioClienteProducto;
import proyecto.entidades.Producto;
import proyecto.entidades.Sede;
import proyecto.entidades.TipoPerfilVendedor;
import proyecto.entidades.TipoMovimientoProduccion;
import proyecto.entidades.Vendedor;
import proyecto.repositorios.ClienteRepository;
import proyecto.repositorios.InventarioProduccionRepository;
import proyecto.repositorios.MovimientoProduccionRepository;
import proyecto.repositorios.PrecioClienteProductoRepository;
import proyecto.repositorios.ProductoRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.repositorios.VendedorRepository;
import proyecto.servicios.interfaces.VentaServicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    @Mock
    private InventarioProduccionRepository inventarioProduccionRepository;
    @Mock
    private MovimientoProduccionRepository movimientoProduccionRepository;
    @Mock
    private SedeRepository sedeRepository;
    @Mock
    private VentaServicio ventaServicio;

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

    @Test
    void actualizarClienteDebePersistirCambios() {
        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);

        Vendedor produccion = new Vendedor();
        produccion.setCorreo("prod@correo.com");
        produccion.setTipoPerfil(TipoPerfilVendedor.PRODUCCION);
        produccion.setEmpresa(empresa);

        Cliente cliente = new Cliente();
        cliente.setId(10L);
        cliente.setNombre("Cliente Viejo");
        cliente.setTelefono("111");
        cliente.setDocumento("DOC1");
        cliente.setEmpresa(empresa);
        cliente.setActivo(true);

        when(vendedorRepository.findByCorreo("prod@correo.com")).thenReturn(Optional.of(produccion));
        when(clienteRepository.findByIdAndEmpresaNit(10L, 900123456L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        var respuesta = produccionServicio.actualizarCliente(
                "prod@correo.com",
                10L,
                new ClienteActualizarDTO("Cliente Nuevo", "222", "DOC2")
        );

        assertEquals("Cliente Nuevo", respuesta.nombre());
        assertEquals("222", respuesta.telefono());
        assertEquals("DOC2", respuesta.documento());
    }

    @Test
    void eliminarClienteDebeDesactivarClienteYPrecios() {
        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);

        Vendedor produccion = new Vendedor();
        produccion.setCorreo("prod@correo.com");
        produccion.setTipoPerfil(TipoPerfilVendedor.PRODUCCION);
        produccion.setEmpresa(empresa);

        Cliente cliente = new Cliente();
        cliente.setId(10L);
        cliente.setEmpresa(empresa);
        cliente.setActivo(true);

        PrecioClienteProducto precio = new PrecioClienteProducto();
        precio.setId(100L);
        precio.setActivo(true);
        precio.setCliente(cliente);

        when(vendedorRepository.findByCorreo("prod@correo.com")).thenReturn(Optional.of(produccion));
        when(clienteRepository.findByIdAndEmpresaNit(10L, 900123456L)).thenReturn(Optional.of(cliente));
        when(precioClienteProductoRepository.findByClienteId(10L)).thenReturn(List.of(precio));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));
        when(precioClienteProductoRepository.save(any(PrecioClienteProducto.class))).thenAnswer(inv -> inv.getArgument(0));

        produccionServicio.eliminarCliente("prod@correo.com", 10L);

        assertFalse(cliente.getActivo());
        assertFalse(precio.getActivo());
        verify(clienteRepository).save(cliente);
        verify(precioClienteProductoRepository).save(precio);
    }

    @Test
    void listarProductosDebeFiltrarPorSedeDeProduccion() {
        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);

        Sede sede = new Sede();
        sede.setId(5L);
        sede.setEmpresa(empresa);

        Vendedor produccion = new Vendedor();
        produccion.setCorreo("prod@correo.com");
        produccion.setTipoPerfil(TipoPerfilVendedor.PRODUCCION);
        produccion.setEmpresa(empresa);
        produccion.setSede(sede);

        Producto producto = new Producto();
        producto.setCodigo(30L);
        producto.setNombre("Producto Listado");
        producto.setPrecioVenta(20000.0);

        InventarioProduccion inventario = new InventarioProduccion();
        inventario.setProducto(producto);
        inventario.setSede(sede);
        inventario.setStockActual(0);
        inventario.setProducidoAcumulado(0);
        inventario.setDespachadoAcumulado(0);

        when(vendedorRepository.findByCorreo("prod@correo.com")).thenReturn(Optional.of(produccion));
        when(inventarioProduccionRepository.findBySedeIdAndProductoActivoTrueOrderByProductoCodigoAsc(5L))
                .thenReturn(List.of(inventario));

        var respuesta = produccionServicio.listarProductos("prod@correo.com");

        assertEquals(1, respuesta.size());
        assertEquals(30L, respuesta.get(0).id());
    }

    @Test
    void actualizarProductoDebePersistirCambios() {
        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);

        Vendedor produccion = new Vendedor();
        produccion.setCorreo("prod@correo.com");
        produccion.setTipoPerfil(TipoPerfilVendedor.PRODUCCION);
        produccion.setEmpresa(empresa);

        Producto producto = new Producto();
        producto.setCodigo(30L);
        producto.setNombre("Producto Viejo");
        producto.setPrecioVenta(10000.0);
        producto.setEmpresa(empresa);
        producto.setActivo(true);

        when(vendedorRepository.findByCorreo("prod@correo.com")).thenReturn(Optional.of(produccion));
        when(productoRepository.findByCodigoAndEmpresaNit(30L, 900123456L)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        var respuesta = produccionServicio.actualizarProducto(
                "prod@correo.com",
                30L,
                new ProductoProduccionRequestDTO("Producto Nuevo", null, 12000.0, null, null)
        );

        assertEquals("Producto Nuevo", respuesta.nombre());
        assertEquals(12000.0, respuesta.precioBase());
    }

    @Test
    void crearProductoDebeCrearInventarioDeProduccion() {
        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);

        Sede sede = new Sede();
        sede.setId(5L);
        sede.setEmpresa(empresa);

        Vendedor produccion = new Vendedor();
        produccion.setCorreo("prod@correo.com");
        produccion.setTipoPerfil(TipoPerfilVendedor.PRODUCCION);
        produccion.setEmpresa(empresa);
        produccion.setSede(sede);

        when(vendedorRepository.findByCorreo("prod@correo.com")).thenReturn(Optional.of(produccion));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> {
            Producto p = inv.getArgument(0);
            p.setCodigo(40L);
            return p;
        });
        when(inventarioProduccionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var respuesta = produccionServicio.crearProducto(
                "prod@correo.com",
                new ProductoProduccionRequestDTO("Producto Creado", "Desc", 4500.0, 4500.0, 4500.0)
        );

        assertEquals(40L, respuesta.id());
        assertEquals("Producto Creado", respuesta.nombre());
        assertEquals(4500.0, respuesta.precioBase());
        verify(inventarioProduccionRepository).save(any());
    }

    @Test
    void eliminarProductoDebeDesactivarlo() {
        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);

        Vendedor produccion = new Vendedor();
        produccion.setCorreo("prod@correo.com");
        produccion.setTipoPerfil(TipoPerfilVendedor.PRODUCCION);
        produccion.setEmpresa(empresa);

        Producto producto = new Producto();
        producto.setCodigo(30L);
        producto.setEmpresa(empresa);
        producto.setActivo(true);

        when(vendedorRepository.findByCorreo("prod@correo.com")).thenReturn(Optional.of(produccion));
        when(productoRepository.findByCodigoAndEmpresaNit(30L, 900123456L)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        produccionServicio.eliminarProducto("prod@correo.com", 30L);

        assertFalse(producto.getActivo());
        verify(productoRepository).save(producto);
    }

    @Test
    void registrarProduccionMultipleDebeRegistrarTodosLosProductos() {
        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);

        Sede sede = new Sede();
        sede.setId(5L);
        sede.setEmpresa(empresa);

        Vendedor produccion = new Vendedor();
        produccion.setCorreo("prod@correo.com");
        produccion.setTipoPerfil(TipoPerfilVendedor.PRODUCCION);
        produccion.setEmpresa(empresa);
        produccion.setSede(sede);

        Producto productoA = new Producto();
        productoA.setCodigo(40L);
        productoA.setNombre("Producto A");
        productoA.setEmpresa(empresa);

        Producto productoB = new Producto();
        productoB.setCodigo(41L);
        productoB.setNombre("Producto B");
        productoB.setEmpresa(empresa);

        when(vendedorRepository.findByCorreo("prod@correo.com")).thenReturn(Optional.of(produccion));
        when(productoRepository.findById(40L)).thenReturn(Optional.of(productoA));
        when(productoRepository.findById(41L)).thenReturn(Optional.of(productoB));
        when(inventarioProduccionRepository.findByProductoCodigoAndSedeId(40L, 5L)).thenReturn(Optional.empty());
        when(inventarioProduccionRepository.findByProductoCodigoAndSedeId(41L, 5L)).thenReturn(Optional.empty());
        when(inventarioProduccionRepository.save(any(InventarioProduccion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(movimientoProduccionRepository.save(any(MovimientoProduccion.class))).thenAnswer(inv -> inv.getArgument(0));

        var respuesta = produccionServicio.registrarProduccionMultiple(
                "prod@correo.com",
                new ProduccionRegistroMultipleDTO(
                        List.of(
                                new ProduccionRegistroItemDTO(40L, 10),
                                new ProduccionRegistroItemDTO(41L, 7)
                        ),
                        "Lote de la manana"
                )
        );

        assertEquals("Produccion registrada correctamente", respuesta);
        verify(inventarioProduccionRepository, times(2)).save(any(InventarioProduccion.class));
        verify(movimientoProduccionRepository, times(2)).save(any(MovimientoProduccion.class));
    }

    @Test
    void listarInventarioAjustableDebeDevolverProductosDeLaSedeDeProduccion() {
        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);

        Sede sede = new Sede();
        sede.setId(5L);
        sede.setEmpresa(empresa);

        Vendedor produccion = new Vendedor();
        produccion.setCorreo("prod@correo.com");
        produccion.setTipoPerfil(TipoPerfilVendedor.PRODUCCION);
        produccion.setEmpresa(empresa);
        produccion.setSede(sede);

        Producto producto = new Producto();
        producto.setCodigo(77L);
        producto.setNombre("Pastel de pollo");
        producto.setActivo(true);
        producto.setEmpresa(empresa);

        InventarioProduccion inventario = new InventarioProduccion();
        inventario.setProducto(producto);
        inventario.setSede(sede);
        inventario.setStockActual(450);

        when(vendedorRepository.findByCorreo("prod@correo.com")).thenReturn(Optional.of(produccion));
        when(inventarioProduccionRepository.findBySedeIdAndProductoActivoTrueOrderByProductoCodigoAsc(5L))
                .thenReturn(List.of(inventario));

        var respuesta = produccionServicio.listarInventarioAjustable("prod@correo.com");

        assertEquals(1, respuesta.items().size());
        assertEquals(77L, respuesta.items().get(0).productoId());
        assertEquals("Pastel de pollo", respuesta.items().get(0).productoNombre());
        assertEquals(450, respuesta.items().get(0).stockActual());
    }

    @Test
    void ajustarInventarioManualDebeActualizarLosStocksSolicitados() {
        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);

        Sede sede = new Sede();
        sede.setId(5L);
        sede.setEmpresa(empresa);

        Vendedor produccion = new Vendedor();
        produccion.setCorreo("prod@correo.com");
        produccion.setTipoPerfil(TipoPerfilVendedor.PRODUCCION);
        produccion.setEmpresa(empresa);
        produccion.setSede(sede);

        Producto producto = new Producto();
        producto.setCodigo(77L);
        producto.setNombre("Pastel de pollo");
        producto.setActivo(true);
        producto.setEmpresa(empresa);

        InventarioProduccion inventario = new InventarioProduccion();
        inventario.setProducto(producto);
        inventario.setSede(sede);
        inventario.setStockActual(450);

        when(vendedorRepository.findByCorreo("prod@correo.com")).thenReturn(Optional.of(produccion));
        when(inventarioProduccionRepository.findByProductoCodigoAndSedeIdAndProductoActivoTrue(77L, 5L))
                .thenReturn(Optional.of(inventario));
        when(inventarioProduccionRepository.save(any(InventarioProduccion.class))).thenAnswer(inv -> inv.getArgument(0));

        var respuesta = produccionServicio.ajustarInventarioManual(
                "prod@correo.com",
                new ProduccionAjusteManualRequestDTO(List.of(
                        new ProduccionAjusteManualRequestItemDTO(77L, 430)
                ))
        );

        assertEquals(1, respuesta.actualizados());
        assertEquals(1, respuesta.resultado().size());
        assertEquals(77L, respuesta.resultado().get(0).productoId());
        assertEquals(450, respuesta.resultado().get(0).stockAnterior());
        assertEquals(430, respuesta.resultado().get(0).stockNuevo());
        assertEquals(430, inventario.getStockActual());
        verify(inventarioProduccionRepository).save(inventario);
        verify(movimientoProduccionRepository).save(argThat(movimiento ->
                movimiento.getTipo() == TipoMovimientoProduccion.AJUSTE
                        && movimiento.getCantidad() == -20
                        && movimiento.getProducto() == producto
        ));
    }

    @Test
    void informeHistoricoDebeDescontarMovimientosPosterioresDelStockActual() {
        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);

        Sede sede = new Sede();
        sede.setId(14L);
        sede.setEmpresa(empresa);

        Vendedor produccion = new Vendedor();
        produccion.setCorreo("prod@correo.com");
        produccion.setTipoPerfil(TipoPerfilVendedor.PRODUCCION);
        produccion.setEmpresa(empresa);
        produccion.setSede(sede);

        Producto producto = new Producto();
        producto.setCodigo(453L);
        producto.setNombre("Pastel de pollo");
        producto.setActivo(true);

        InventarioProduccion inventario = new InventarioProduccion();
        inventario.setProducto(producto);
        inventario.setSede(sede);
        inventario.setStockActual(515);

        MovimientoProduccion produccionViernes = movimiento(producto, sede, TipoMovimientoProduccion.PRODUCCION, 503);
        MovimientoProduccion despachoViernes = movimiento(producto, sede, TipoMovimientoProduccion.DESPACHO, 516);
        LocalDate fecha = LocalDate.of(2026, 7, 10);
        when(vendedorRepository.findByCorreo("prod@correo.com")).thenReturn(Optional.of(produccion));
        when(movimientoProduccionRepository.findBySedeIdAndFechaBetweenOrderByFechaAsc(
                14L, fecha.atStartOfDay(), fecha.atTime(java.time.LocalTime.MAX)))
                .thenReturn(List.of(produccionViernes, despachoViernes));
        when(movimientoProduccionRepository.sumarVariacionStockPosterior(
                14L,
                fecha.atTime(java.time.LocalTime.MAX),
                TipoMovimientoProduccion.PRODUCCION,
                TipoMovimientoProduccion.DESPACHO))
                .thenReturn(List.<Object[]>of(new Object[]{453L, -70L}));
        when(inventarioProduccionRepository.findBySedeIdAndProductoActivoTrueOrderByProductoCodigoAsc(14L))
                .thenReturn(List.of(inventario));

        var informe = produccionServicio.obtenerInformeDiario("prod@correo.com", fecha);
        var resumen = informe.productos().get(0);

        assertEquals(598, resumen.stockInicial());
        assertEquals(503, resumen.producido());
        assertEquals(516, resumen.despachado());
        assertEquals(585, resumen.stockFinal());
    }

    private MovimientoProduccion movimiento(
            Producto producto,
            Sede sede,
            TipoMovimientoProduccion tipo,
            int cantidad
    ) {
        MovimientoProduccion movimiento = new MovimientoProduccion();
        movimiento.setProducto(producto);
        movimiento.setSede(sede);
        movimiento.setTipo(tipo);
        movimiento.setCantidad(cantidad);
        return movimiento;
    }

    @Test
    void ajustarInventarioManualDebeOmitirItemsSinCambios() {
        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);

        Sede sede = new Sede();
        sede.setId(5L);
        sede.setEmpresa(empresa);

        Vendedor produccion = new Vendedor();
        produccion.setCorreo("prod@correo.com");
        produccion.setTipoPerfil(TipoPerfilVendedor.PRODUCCION);
        produccion.setEmpresa(empresa);
        produccion.setSede(sede);

        Producto producto = new Producto();
        producto.setCodigo(77L);
        producto.setNombre("Pastel de pollo");
        producto.setActivo(true);
        producto.setEmpresa(empresa);

        InventarioProduccion inventario = new InventarioProduccion();
        inventario.setProducto(producto);
        inventario.setSede(sede);
        inventario.setStockActual(450);

        when(vendedorRepository.findByCorreo("prod@correo.com")).thenReturn(Optional.of(produccion));
        when(inventarioProduccionRepository.findByProductoCodigoAndSedeIdAndProductoActivoTrue(77L, 5L))
                .thenReturn(Optional.of(inventario));

        var respuesta = produccionServicio.ajustarInventarioManual(
                "prod@correo.com",
                new ProduccionAjusteManualRequestDTO(List.of(
                        new ProduccionAjusteManualRequestItemDTO(77L, 450)
                ))
        );

        assertEquals(0, respuesta.actualizados());
        assertEquals(0, respuesta.resultado().size());
        verify(inventarioProduccionRepository, never()).save(any(InventarioProduccion.class));
    }

    @Test
    void ajustarInventarioManualDebeRechazarProductosRepetidos() {
        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);

        Sede sede = new Sede();
        sede.setId(5L);
        sede.setEmpresa(empresa);

        Vendedor produccion = new Vendedor();
        produccion.setCorreo("prod@correo.com");
        produccion.setTipoPerfil(TipoPerfilVendedor.PRODUCCION);
        produccion.setEmpresa(empresa);
        produccion.setSede(sede);

        when(vendedorRepository.findByCorreo("prod@correo.com")).thenReturn(Optional.of(produccion));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                produccionServicio.ajustarInventarioManual(
                        "prod@correo.com",
                        new ProduccionAjusteManualRequestDTO(List.of(
                                new ProduccionAjusteManualRequestItemDTO(77L, 430),
                                new ProduccionAjusteManualRequestItemDTO(77L, 420)
                        ))
                )
        );

        assertEquals("Hay productos repetidos en la solicitud: 77", error.getMessage());
    }

    @Test
    void obtenerEstadoAdminPinDebeIndicarSiLaSedeYaTienePin() {
        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);

        Sede sede = new Sede();
        sede.setId(5L);
        sede.setEmpresa(empresa);
        sede.setAdminPinHash("$2a$10$abc");

        Vendedor produccion = new Vendedor();
        produccion.setCorreo("prod@correo.com");
        produccion.setTipoPerfil(TipoPerfilVendedor.PRODUCCION);
        produccion.setEmpresa(empresa);
        produccion.setSede(sede);

        when(vendedorRepository.findByCorreo("prod@correo.com")).thenReturn(Optional.of(produccion));

        var respuesta = produccionServicio.obtenerEstadoAdminPin("prod@correo.com");

        assertTrue(respuesta.configurado());
    }

    @Test
    void actualizarAdminPinDebeCrearPinCuandoLaSedeNoTieneUno() {
        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);

        Sede sede = new Sede();
        sede.setId(5L);
        sede.setEmpresa(empresa);

        Vendedor produccion = new Vendedor();
        produccion.setCorreo("prod@correo.com");
        produccion.setTipoPerfil(TipoPerfilVendedor.PRODUCCION);
        produccion.setEmpresa(empresa);
        produccion.setSede(sede);

        when(vendedorRepository.findByCorreo("prod@correo.com")).thenReturn(Optional.of(produccion));
        when(sedeRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(sede));
        when(sedeRepository.save(any(Sede.class))).thenAnswer(inv -> inv.getArgument(0));

        var respuesta = produccionServicio.actualizarAdminPin(
                "prod@correo.com",
                new proyecto.dto.AdminPinActualizarRequestDTO(null, "1234")
        );

        assertEquals("PIN actualizado correctamente", respuesta.mensaje());
        assertNotNull(sede.getAdminPinHash());
        assertNotEquals("1234", sede.getAdminPinHash());
        verify(sedeRepository).save(sede);
    }

    @Test
    void validarAdminPinDebeAceptarCuandoElPinCoincide() {
        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);

        Sede sede = new Sede();
        sede.setId(5L);
        sede.setEmpresa(empresa);
        sede.setAdminPinHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("1234"));
        sede.setAdminPinIntentosFallidos(2);

        Vendedor produccion = new Vendedor();
        produccion.setCorreo("prod@correo.com");
        produccion.setTipoPerfil(TipoPerfilVendedor.PRODUCCION);
        produccion.setEmpresa(empresa);
        produccion.setSede(sede);

        when(vendedorRepository.findByCorreo("prod@correo.com")).thenReturn(Optional.of(produccion));
        when(sedeRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(sede));
        when(sedeRepository.save(any(Sede.class))).thenAnswer(inv -> inv.getArgument(0));

        var respuesta = produccionServicio.validarAdminPin(
                "prod@correo.com",
                new proyecto.dto.AdminPinValidacionRequestDTO("1234")
        );

        assertTrue(respuesta.valido());
        assertEquals("PIN valido", respuesta.mensaje());
        assertEquals(0, sede.getAdminPinIntentosFallidos());
        assertNull(sede.getAdminPinBloqueadoHasta());
    }

    @Test
    void validarAdminPinDebeBloquearTemporalmenteDespuesDeMaximosIntentos() {
        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);

        Sede sede = new Sede();
        sede.setId(5L);
        sede.setEmpresa(empresa);
        sede.setAdminPinHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("1234"));
        sede.setAdminPinIntentosFallidos(4);

        Vendedor produccion = new Vendedor();
        produccion.setCorreo("prod@correo.com");
        produccion.setTipoPerfil(TipoPerfilVendedor.PRODUCCION);
        produccion.setEmpresa(empresa);
        produccion.setSede(sede);

        when(vendedorRepository.findByCorreo("prod@correo.com")).thenReturn(Optional.of(produccion));
        when(sedeRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(sede));
        when(sedeRepository.save(any(Sede.class))).thenAnswer(inv -> inv.getArgument(0));

        var respuesta = produccionServicio.validarAdminPin(
                "prod@correo.com",
                new proyecto.dto.AdminPinValidacionRequestDTO("9999")
        );

        assertFalse(respuesta.valido());
        assertEquals("PIN bloqueado temporalmente. Intenta mas tarde", respuesta.mensaje());
        assertEquals(0, sede.getAdminPinIntentosFallidos());
        assertNotNull(sede.getAdminPinBloqueadoHasta());
        assertTrue(sede.getAdminPinBloqueadoHasta().isAfter(LocalDateTime.now().minusMinutes(1)));
    }
}
