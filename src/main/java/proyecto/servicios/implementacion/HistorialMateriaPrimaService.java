package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class HistorialMateriaPrimaService {
    private final JdbcTemplate jdbc;

    public record Movimiento(long id, LocalDateTime fecha, String tipo, double stockAnterior,
                             double stockNuevo, Long productoId, String observacion) {}
    public record Dia(LocalDate fecha, boolean completo, Double stockInicial, double entradas,
                      double perdidas, double ventaUnitaria, double ventaCombo, double salidasManuales,
                      double ajustes, Double stockFinal) {}
    public record Reporte(LocalDateTime corte, LocalDateTime inicioHistorial, double saldoApertura,
                          double stockActual, List<Dia> dias, List<Movimiento> movimientos) {}

    public List<Map<String, Object>> listar(long sede) {
        return jdbc.queryForList("""
            SELECT mp.codigo, mp.nombre, mp.unidad_base, m.cantidad_actual_ml
            FROM materia_prima_sede m JOIN materia_prima mp ON mp.codigo=m.materia_prima_id
            WHERE m.sede_id=? ORDER BY mp.nombre
            """, sede);
    }

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Reporte consultar(long sede, long materia, LocalDate desde, LocalDate hasta) {
        LocalDateTime corte = LocalDateTime.now(ZoneId.of("America/Bogota"));
        if (desde == null || hasta == null || desde.isAfter(hasta)
                || hasta.isAfter(corte.toLocalDate()) || java.time.temporal.ChronoUnit.DAYS.between(desde, hasta) > 366) {
            throw new IllegalArgumentException("Seleccione un rango de hasta 367 dias, sin fechas futuras");
        }
        List<Map<String, Object>> saldos = jdbc.queryForList(
                "SELECT id,cantidad_actual_ml FROM materia_prima_sede WHERE sede_id=? AND materia_prima_id=?", sede, materia);
        if (saldos.isEmpty()) throw new IllegalArgumentException("Materia prima no encontrada en la sede");
        long inventario = ((Number) saldos.get(0).get("id")).longValue();
        double stock = ((Number) saldos.get(0).get("cantidad_actual_ml")).doubleValue();
        List<Movimiento> historia = jdbc.query("""
            SELECT id,fecha,tipo,stock_anterior,stock_nuevo,producto_id,observacion
            FROM movimiento_materia_prima WHERE materia_prima_sede_id=? ORDER BY fecha,id
            """, (rs, n) -> new Movimiento(rs.getLong(1), rs.getTimestamp(2).toLocalDateTime(), rs.getString(3),
                rs.getDouble(4), rs.getDouble(5), (Long) rs.getObject(6), rs.getString(7)), inventario);
        return calcular(corte, stock, historia, desde, hasta);
    }

    public static Reporte calcular(LocalDateTime corte, double stock, List<Movimiento> historia,
                                   LocalDate desde, LocalDate hasta) {
        LocalDateTime inicio = historia.isEmpty() ? null : historia.get(0).fecha();
        double saldo = historia.isEmpty() ? stock : historia.get(0).stockAnterior();
        double apertura = saldo;
        List<Dia> dias = new ArrayList<>();
        int index = 0;
        for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
            while (index < historia.size() && historia.get(index).fecha().isBefore(fecha.atStartOfDay())) {
                saldo = historia.get(index++).stockNuevo();
            }
            boolean conocido = inicio != null && !fecha.isBefore(inicio.toLocalDate());
            boolean completo = inicio != null && !inicio.isAfter(fecha.atStartOfDay());
            Double inicial = completo ? saldo : null;
            double entradas=0, perdidas=0, unitaria=0, combo=0, manual=0, ajustes=0;
            while (index < historia.size() && historia.get(index).fecha().isBefore(fecha.plusDays(1).atStartOfDay())) {
                Movimiento m = historia.get(index++);
                double delta = m.stockNuevo() - m.stockAnterior();
                switch (m.tipo()) {
                    case "ENTRADA" -> entradas += delta;
                    case "PERDIDA" -> perdidas -= delta;
                    case "VENTA_UNITARIA" -> unitaria -= delta;
                    case "VENTA_COMBO" -> combo -= delta;
                    case "SALIDA_MANUAL" -> manual -= delta;
                    case "APERTURA" -> { }
                    default -> ajustes += delta;
                }
                saldo = m.stockNuevo();
            }
            dias.add(new Dia(fecha, completo, inicial, entradas, perdidas, unitaria, combo, manual, ajustes,
                    conocido ? saldo : null));
        }
        List<Movimiento> movimientos = historia.stream().filter(m -> !m.fecha().toLocalDate().isBefore(desde)
                && !m.fecha().toLocalDate().isAfter(hasta)).toList();
        return new Reporte(corte, inicio, apertura, stock, dias, movimientos);
    }
}
