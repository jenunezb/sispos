package proyecto.servicios.implementacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import proyecto.dto.VentaHoraResumenProjection;
import proyecto.dto.VentaMesResumenProjection;
import proyecto.repositorios.VentaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteVentasAdminServicioImplTest {

    @Mock
    private VentaRepository ventaRepository;

    @InjectMocks
    private ReporteVentasAdminServicioImpl reporteVentasAdminServicio;

    @Test
    void obtenerVentasPorMesDebeCompletarMesesSinVentasConCeros() {
        when(ventaRepository.resumenVentasPorMes(eq(List.of(1L)), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(
                        mes(2026, 1, 100000.0, 10L),
                        mes(2026, 3, 50000.0, 4L)
                ));

        var respuesta = reporteVentasAdminServicio.obtenerVentasPorMes(
                List.of(1L),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 31)
        );

        assertEquals(3, respuesta.size());
        assertEquals("Enero 2026", respuesta.get(0).periodo());
        assertEquals(100000.0, respuesta.get(0).totalVentas());
        assertEquals("Febrero 2026", respuesta.get(1).periodo());
        assertEquals(0.0, respuesta.get(1).totalVentas());
        assertEquals(0L, respuesta.get(1).cantidadVentas());
        assertEquals("Marzo 2026", respuesta.get(2).periodo());
        assertEquals(50000.0, respuesta.get(2).totalVentas());
    }

    @Test
    void obtenerVentasPorHoraDebeRetornarLas24Horas() {
        when(ventaRepository.resumenVentasPorHora(eq(List.of(2L)), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(
                        hora(9, 25000.0, 3L),
                        hora(15, 40000.0, 5L)
                ));

        var respuesta = reporteVentasAdminServicio.obtenerVentasPorHora(
                List.of(2L),
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 1)
        );

        assertEquals(24, respuesta.size());
        assertEquals("09:00", respuesta.get(9).etiquetaHora());
        assertEquals(25000.0, respuesta.get(9).totalVentas());
        assertEquals(0.0, respuesta.get(10).totalVentas());
        assertEquals("15:00", respuesta.get(15).etiquetaHora());
        assertEquals(40000.0, respuesta.get(15).totalVentas());
    }

    @Test
    void obtenerCrecimientoMensualDebeCalcularPorcentajeContraMesAnterior() {
        when(ventaRepository.resumenVentasPorMes(eq(List.of(3L)), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(
                        mes(2026, 1, 0.0, 0L),
                        mes(2026, 2, 100000.0, 10L),
                        mes(2026, 3, 80000.0, 8L)
                ));

        var respuesta = reporteVentasAdminServicio.obtenerCrecimientoMensual(
                List.of(3L),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 31)
        );

        assertEquals(3, respuesta.size());
        assertNull(respuesta.get(0).crecimientoPorcentual());
        assertNull(respuesta.get(1).crecimientoPorcentual());
        assertEquals(-20.0, respuesta.get(2).crecimientoPorcentual());
    }

    private VentaMesResumenProjection mes(Integer anio, Integer mes, Double totalVentas, Long cantidadVentas) {
        return new VentaMesResumenProjection() {
            @Override
            public Integer getAnio() {
                return anio;
            }

            @Override
            public Integer getMes() {
                return mes;
            }

            @Override
            public Double getTotalVentas() {
                return totalVentas;
            }

            @Override
            public Long getCantidadVentas() {
                return cantidadVentas;
            }
        };
    }

    private VentaHoraResumenProjection hora(Integer hora, Double totalVentas, Long cantidadVentas) {
        return new VentaHoraResumenProjection() {
            @Override
            public Integer getHora() {
                return hora;
            }

            @Override
            public Double getTotalVentas() {
                return totalVentas;
            }

            @Override
            public Long getCantidadVentas() {
                return cantidadVentas;
            }
        };
    }
}
