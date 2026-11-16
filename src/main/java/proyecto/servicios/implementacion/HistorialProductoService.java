package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import static proyecto.servicios.implementacion.HistorialMateriaPrimaService.*;

@Service @RequiredArgsConstructor
public class HistorialProductoService {
    private final JdbcTemplate jdbc;
    public record MovimientoProducto(long id, LocalDateTime fecha, String tipo, Double stockAnterior,
            Double stockNuevo, Long productoId, String observacion, double cantidad) {}
    public record ReporteProducto(LocalDateTime corte, LocalDateTime inicioHistorial, Double saldoApertura,
            Double stockActual, List<Dia> dias, List<MovimientoProducto> movimientos, boolean conReceta,
            List<Map<String,Object>> materiasPrimas, Double disponibilidad) {}

    public List<Map<String,Object>> listar(long sede) {
        return jdbc.queryForList("""
            SELECT p.codigo,p.nombre,'UNIDADES' AS unidad_base,
                   EXISTS(SELECT 1 FROM producto_materia_prima r WHERE r.producto_id=p.codigo) AS con_receta
            FROM inventario i JOIN producto p ON p.codigo=i.producto_id JOIN sede s ON s.id=i.sede_id
            WHERE i.sede_id=? AND p.empresa_id=s.empresa_id AND p.activo=true ORDER BY p.nombre,p.codigo
            """,sede);
    }

    @Transactional(readOnly=true, isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public ReporteProducto consultar(long sede,long producto,LocalDate desde,LocalDate hasta) {
        LocalDateTime corte=LocalDateTime.now(ZoneId.of("America/Bogota"));
        if (desde==null || hasta==null || desde.isAfter(hasta) || hasta.isAfter(corte.toLocalDate())
                || java.time.temporal.ChronoUnit.DAYS.between(desde,hasta)>366)
            throw new IllegalArgumentException("Rango de fechas invalido");
        var inventarios=jdbc.queryForList("""
            SELECT i.id,i.stock_actual,p.nombre FROM inventario i JOIN producto p ON p.codigo=i.producto_id
            JOIN sede s ON s.id=i.sede_id WHERE i.sede_id=? AND p.codigo=? AND p.empresa_id=s.empresa_id
            """,sede,producto);
        if(inventarios.isEmpty()) throw new IllegalArgumentException("Producto no encontrado en la sede");
        var insumos=jdbc.queryForList("""
            SELECT mp.codigo,mp.nombre,r.ml_consumidos,coalesce(m.cantidad_actual_ml,0) AS stock_actual,
                   coalesce(m.activa,false) AND mp.activa AS activa
            FROM producto_materia_prima r JOIN materia_prima mp ON mp.codigo=r.materia_prima_id
            LEFT JOIN materia_prima_sede m ON m.materia_prima_id=mp.codigo AND m.sede_id=?
            WHERE r.producto_id=? ORDER BY mp.nombre
            """,sede,producto);
        if(!insumos.isEmpty()) return receta(corte,sede,producto,desde,hasta,insumos,
                inventarios.get(0).get("nombre").toString().toLowerCase(Locale.ROOT).contains("combo"));
        long id=((Number)inventarios.get(0).get("id")).longValue();
        double stock=((Number)inventarios.get(0).get("stock_actual")).doubleValue();
        var historia=jdbc.query("""
            SELECT id,fecha,tipo,stock_anterior,stock_nuevo,observacion FROM movimiento_stock_producto
            WHERE inventario_id=? ORDER BY fecha,id
            """,(rs,n)->new Movimiento(rs.getLong(1),rs.getTimestamp(2).toLocalDateTime(),rs.getString(3),
                    rs.getDouble(4),rs.getDouble(5),producto,rs.getString(6)),id);
        var r=calcular(corte,stock,historia,desde,hasta);
        var detalle=r.movimientos().stream().map(m->new MovimientoProducto(m.id(),m.fecha(),m.tipo(),
                m.stockAnterior(),m.stockNuevo(),producto,m.observacion(),m.stockNuevo()-m.stockAnterior())).toList();
        return new ReporteProducto(corte,r.inicioHistorial(),r.saldoApertura(),stock,r.dias(),detalle,false,List.of(),null);
    }

    private ReporteProducto receta(LocalDateTime corte,long sede,long producto,LocalDate desde,LocalDate hasta,
                                    List<Map<String,Object>> insumos,boolean combo) {
        var movimientos=jdbc.query("""
            SELECT id,fecha,tipo,cantidad,observacion FROM movimiento_inventario
            WHERE sede_id=? AND producto_id=? AND fecha>=? AND fecha<? ORDER BY fecha,id
            """,(rs,n)->{
                String tipo=rs.getString(3),obs=rs.getString(5);
                if("SALIDA".equals(tipo)) tipo="Venta de producto".equals(obs)
                        ? (combo?"VENTA_COMBO":"VENTA_UNITARIA") : "SALIDA_MANUAL";
                double cantidad=rs.getDouble(4)*("ENTRADA".equals(tipo)?1:-1);
                return new MovimientoProducto(rs.getLong(1),rs.getTimestamp(2).toLocalDateTime(),tipo,
                        null,null,producto,obs,cantidad);
            },sede,producto,desde.atStartOfDay(),hasta.plusDays(1).atStartOfDay());
        List<Dia> dias=new ArrayList<>();
        for(LocalDate dia=desde;!dia.isAfter(hasta);dia=dia.plusDays(1)) {
            double entrada=0,perdida=0,unitaria=0,combos=0,manual=0;
            for(var m:movimientos) if(m.fecha().toLocalDate().equals(dia)) {
                switch(m.tipo()) {
                    case "ENTRADA" -> entrada+=m.cantidad();
                    case "PERDIDA" -> perdida-=m.cantidad();
                    case "VENTA_UNITARIA" -> unitaria-=m.cantidad();
                    case "VENTA_COMBO" -> combos-=m.cantidad();
                    default -> manual-=m.cantidad();
                }
            }
            dias.add(new Dia(dia,false,null,entrada,perdida,unitaria,combos,manual,0,null));
        }
        double disponible=insumos.stream().mapToDouble(i->{
            double consumo=((Number)i.get("ml_consumidos")).doubleValue();
            return Boolean.TRUE.equals(i.get("activa")) && consumo>0
                    ? Math.floor(((Number)i.get("stock_actual")).doubleValue()/consumo):0;
        }).min().orElse(0);
        return new ReporteProducto(corte,null,null,null,dias,movimientos,true,insumos,Math.max(0,disponible));
    }
}
