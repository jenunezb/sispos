package proyecto.servicios.implementacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import proyecto.dto.BalanceGeneralDTO;
import proyecto.repositorios.DetalleVentaRepository;
import proyecto.repositorios.InventarioRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.repositorios.VentaRepository;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @InjectMocks
    private BalanceServicioImpl balanceServicio;

    @Test
    void balanceGeneralDebeFiltrarPorEmpresa() {
        Long empresaNit = 900123456L;
        LocalDateTime desde = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 1, 31, 23, 59, 59);

        when(ventaRepository.totalVentasEntreFechasPorEmpresa(empresaNit, desde, hasta)).thenReturn(50000.0);
        when(detalleVentaRepository.costoProduccionEntreFechasPorEmpresa(empresaNit, desde, hasta)).thenReturn(20000.0);
        when(ventaRepository.cantidadVentasEntreFechasPorEmpresa(empresaNit, desde, hasta)).thenReturn(10L);
        when(inventarioRepository.valorInventarioPorEmpresa(empresaNit)).thenReturn(3000.0);
        when(inventarioRepository.stockTotalPorEmpresa(empresaNit)).thenReturn(100);
        when(ventaRepository.totalVentasEntreFechasEfectivoPorEmpresa(empresaNit, desde, hasta)).thenReturn(30000.0);
        when(ventaRepository.totalVentasEntreFechasTransferenciaPorEmpresa(empresaNit, desde, hasta)).thenReturn(20000.0);

        BalanceGeneralDTO respuesta = balanceServicio.balanceGeneral(empresaNit, desde, hasta);

        assertEquals(50000.0, respuesta.totalVentas());
        assertEquals(20000.0, respuesta.costoProduccion());
        assertEquals(30000.0, respuesta.utilidadBruta());
        assertEquals(10L, respuesta.cantidadVentas());

        verify(ventaRepository).totalVentasEntreFechasPorEmpresa(empresaNit, desde, hasta);
        verify(detalleVentaRepository).costoProduccionEntreFechasPorEmpresa(empresaNit, desde, hasta);
    }
}
