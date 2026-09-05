package proyecto.servicios.implementacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import proyecto.dto.BalanceGeneralDTO;
import proyecto.entidades.CajaTurno;
import proyecto.entidades.EstadoCaja;
import proyecto.entidades.Sede;
import proyecto.repositorios.CajaTurnoRepository;
import proyecto.repositorios.DetalleVentaRepository;
import proyecto.repositorios.GastoDiarioRepository;
import proyecto.repositorios.InventarioRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.repositorios.VentaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceServicioImplTest {

    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private DetalleVentaRepository detalleVentaRepository;
    @Mock
    private InventarioRepository inventarioRepository;
    @Mock
    private SedeRepository sedeRepository;
    @Mock
    private GastoDiarioRepository gastoDiarioRepository;
    @Mock
    private SuscripcionFeatureService suscripcionFeatureService;
    @Mock
    private CajaTurnoRepository cajaTurnoRepository;

    @InjectMocks
    private BalanceServicioImpl balanceServicio;

    @Test
    void balanceGeneralDebeFiltrarPorEmpresa() {
        Long empresaNit = 900123456L;
        LocalDateTime desde = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 1, 31, 23, 59, 59);

        Sede sede = new Sede();
        sede.setId(1L);
        sede.setUbicacion("Principal");

        when(sedeRepository.findByEmpresaNit(empresaNit)).thenReturn(List.of(sede));
        when(ventaRepository.totalVentasPorSedeEntreFechas(1L, desde, hasta)).thenReturn(50000.0);
        when(detalleVentaRepository.costoProduccionPorSedeEntreFechas(1L, desde, hasta)).thenReturn(20000.0);
        when(ventaRepository.cantidadVentasPorSedeEntreFechas(1L, desde, hasta)).thenReturn(10L);
        when(inventarioRepository.valorInventarioPorSede(1L)).thenReturn(3000.0);
        when(inventarioRepository.stockPorSede(1L)).thenReturn(100);
        when(ventaRepository.totalVentasEfectivoPorSedeEntreFechas(1L, desde, hasta)).thenReturn(30000.0);
        when(ventaRepository.totalVentasTransferenciaPorSedeEntreFechas(1L, desde, hasta)).thenReturn(20000.0);
        when(suscripcionFeatureService.tieneGastosHabilitados(1L)).thenReturn(true);
        when(gastoDiarioRepository.totalGastosPorSede(1L, desde, hasta)).thenReturn(5000.0);
        when(gastoDiarioRepository.totalGastosPorSedeYModoPago(1L, proyecto.entidades.ModoPago.EFECTIVO, desde, hasta)).thenReturn(3500.0);
        when(gastoDiarioRepository.totalGastosPorSedeYModoPago(1L, proyecto.entidades.ModoPago.TRANSFERENCIA, desde, hasta)).thenReturn(1500.0);

        BalanceGeneralDTO respuesta = balanceServicio.balanceGeneral(empresaNit, desde, hasta);

        assertEquals(50000.0, respuesta.totalVentas());
        assertEquals(20000.0, respuesta.costoProduccion());
        assertEquals(30000.0, respuesta.utilidadBruta());
        assertEquals(5000.0, respuesta.totalGastos());
        assertEquals(3500.0, respuesta.gastosEfectivo());
        assertEquals(1500.0, respuesta.gastosTransferencia());
        assertEquals(26500.0, respuesta.cajaEsperada());
        assertEquals(25000.0, respuesta.utilidadNeta());
        assertEquals(10L, respuesta.cantidadVentas());

        verify(sedeRepository).findByEmpresaNit(empresaNit);
        verify(detalleVentaRepository).costoProduccionPorSedeEntreFechas(1L, desde, hasta);
    }

    @Test
    void balanceActualUsaAperturaDeCajaAunqueCruceMedianoche() {
        Long empresaNit = 900123456L;
        LocalDateTime apertura = LocalDateTime.of(2026, 8, 15, 20, 0);
        Sede sede = new Sede();
        sede.setId(1L);
        sede.setUbicacion("Nocturna");
        CajaTurno caja = new CajaTurno();
        caja.setSede(sede);
        caja.setEstado(EstadoCaja.ABIERTA);
        caja.setFechaApertura(apertura);

        when(sedeRepository.findByEmpresaNit(empresaNit)).thenReturn(List.of(sede));
        when(cajaTurnoRepository.findFirstBySedeIdAndEstadoOrderByFechaAperturaDesc(1L, EstadoCaja.ABIERTA))
                .thenReturn(Optional.of(caja));
        when(ventaRepository.totalVentasPorSedeEntreFechas(eq(1L), eq(apertura), any(LocalDateTime.class)))
                .thenReturn(85000.0);

        var balances = balanceServicio.balancePorSedeHoy(empresaNit);

        assertEquals(85000.0, balances.get(0).totalVentas());
        verify(ventaRepository).totalVentasPorSedeEntreFechas(eq(1L), eq(apertura), any(LocalDateTime.class));
    }
}
