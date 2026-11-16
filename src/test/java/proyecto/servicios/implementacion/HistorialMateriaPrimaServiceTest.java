package proyecto.servicios.implementacion;

import org.junit.jupiter.api.Test;
import proyecto.entidades.*;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static proyecto.servicios.implementacion.HistorialMateriaPrimaService.*;

class HistorialMateriaPrimaServiceTest {
    @Test void conciliaDiasSinMovimientoYNoInventaHistoria() {
        LocalDate dia = LocalDate.of(2026,9,24);
        List<Movimiento> movimientos = List.of(
            new Movimiento(1, dia.atTime(8,0), "APERTURA",79,79,null,""),
            new Movimiento(2, dia.atTime(9,0), "ENTRADA",79,99,null,""),
            new Movimiento(3, dia.atTime(10,0), "VENTA_UNITARIA",99,96,297L,""),
            new Movimiento(4, dia.atTime(11,0), "VENTA_COMBO",96,94,893L,""),
            new Movimiento(5, dia.atTime(12,0), "PERDIDA",94,93,297L,""),
            new Movimiento(6, dia.plusDays(2).atTime(9,0), "AJUSTE",93,95,null,""));
        Reporte r = calcular(dia.plusDays(2).atTime(10,0),95,movimientos,dia.minusDays(1),dia.plusDays(2));
        assertNull(r.dias().get(0).stockFinal());
        assertNull(r.dias().get(1).stockInicial());
        assertEquals(20,r.dias().get(1).entradas());
        assertEquals(3,r.dias().get(1).ventaUnitaria());
        assertEquals(2,r.dias().get(1).ventaCombo());
        assertEquals(1,r.dias().get(1).perdidas());
        assertEquals(93,r.dias().get(2).stockInicial());
        assertEquals(93,r.dias().get(2).stockFinal());
        assertEquals(95,r.dias().get(3).stockFinal());
        assertEquals(2,r.dias().get(3).ajustes());
        Reporte sub = calcular(dia.plusDays(2).atTime(10,0),95,movimientos,dia.plusDays(1),dia.plusDays(2));
        assertEquals(93,sub.dias().get(0).stockInicial());
    }

    @Test void capturaConsumoConRecetaOriginalYRechazaSaldoInvalido() {
        MateriaPrimaSede stock = new MateriaPrimaSede();
        stock.cambiarStock(79,"ENTRADA",null,"Ingreso");
        Producto p = new Producto(); p.setCodigo(893L); p.setNombre("Combo Guayaba + Avena");
        stock.consumirVenta(2,p);
        p.setNombre("PASTEL DE GUAYABA");
        stock.consumirVenta(3,p);
        assertEquals(74,stock.getCantidadActualMl());
        assertEquals("VENTA_COMBO",stock.getMovimientos().get(1).getTipo());
        assertEquals("VENTA_UNITARIA",stock.getMovimientos().get(2).getTipo());
        assertEquals(79,stock.getMovimientos().get(1).getStockAnterior());
        assertThrows(IllegalArgumentException.class, () -> stock.setCantidadActualMl(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> stock.consumirVenta(100,p));
        assertEquals(3,stock.getMovimientos().size());
    }
}
