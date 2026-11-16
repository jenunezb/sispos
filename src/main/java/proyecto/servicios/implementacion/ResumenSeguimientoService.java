package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.List;

@Service @RequiredArgsConstructor
public class ResumenSeguimientoService {
    private final JdbcTemplate jdbc;
    public record Fila(String tipo,long codigo,String nombre,String unidad,boolean conReceta,
                       boolean completo,Double stockInicial,double entradas,double perdidas,
                       double ventaUnitaria,double ventaCombo,double salidasManuales,double ajustes,Double stockFinal) {}
    public record Resumen(LocalDate fecha,LocalDateTime corte,List<Fila> filas) {}

    @Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Resumen hoy(long sede) {
        LocalDateTime corte=LocalDateTime.now(ZoneId.of("America/Bogota"));
        return consultar(sede,corte);
    }

    Resumen consultar(long sede,LocalDateTime corte) {
        // Una sola consulta para toda la sede, incluidos artículos sin movimientos.
        var filas=jdbc.query("""
            WITH parametros AS (SELECT CAST(? AS BIGINT) sede,CAST(? AS TIMESTAMP) inicio,CAST(? AS TIMESTAMP) corte),
            items AS (
                SELECT 'materia'::text tipo,m.id inventario,mp.codigo,mp.nombre,
                       coalesce(mp.unidad_base,'UNIDAD BASE') unidad,false receta,m.cantidad_actual_ml stock
                FROM materia_prima_sede m JOIN materia_prima mp ON mp.codigo=m.materia_prima_id,parametros q
                WHERE m.sede_id=q.sede
                UNION ALL
                SELECT 'producto',i.id,p.codigo,p.nombre,'UNIDADES',
                       EXISTS(SELECT 1 FROM producto_materia_prima r WHERE r.producto_id=p.codigo),i.stock_actual
                FROM inventario i JOIN producto p ON p.codigo=i.producto_id JOIN sede s ON s.id=i.sede_id,parametros q
                WHERE i.sede_id=q.sede AND p.empresa_id=s.empresa_id AND p.activo=true
            ), eventos AS (
                SELECT 'materia'::text articulo,h.materia_prima_sede_id inventario,h.fecha,h.tipo,h.stock_nuevo-h.stock_anterior delta
                FROM movimiento_materia_prima h JOIN materia_prima_sede m ON m.id=h.materia_prima_sede_id,parametros q
                WHERE m.sede_id=q.sede AND h.fecha<=q.corte
                UNION ALL
                SELECT 'producto',h.inventario_id,h.fecha,h.tipo,h.stock_nuevo-h.stock_anterior
                FROM movimiento_stock_producto h JOIN inventario i ON i.id=h.inventario_id,parametros q
                WHERE i.sede_id=q.sede AND h.fecha<=q.corte
            ), totales AS (
                SELECT e.articulo,e.inventario,min(e.fecha) primera,
                    coalesce(sum(delta) FILTER(WHERE e.fecha>=q.inicio),0) cambio,
                    coalesce(sum(delta) FILTER(WHERE e.fecha>=q.inicio AND e.tipo='ENTRADA'),0) entradas,
                    coalesce(-sum(delta) FILTER(WHERE e.fecha>=q.inicio AND e.tipo='PERDIDA'),0) perdidas,
                    coalesce(-sum(delta) FILTER(WHERE e.fecha>=q.inicio AND e.tipo='VENTA_UNITARIA'),0) unitarias,
                    coalesce(-sum(delta) FILTER(WHERE e.fecha>=q.inicio AND e.tipo='VENTA_COMBO'),0) combos,
                    coalesce(-sum(delta) FILTER(WHERE e.fecha>=q.inicio AND e.tipo='SALIDA_MANUAL'),0) manuales,
                    coalesce(sum(delta) FILTER(WHERE e.fecha>=q.inicio AND e.tipo NOT IN
                        ('APERTURA','ENTRADA','PERDIDA','VENTA_UNITARIA','VENTA_COMBO','SALIDA_MANUAL')),0) ajustes
                FROM eventos e CROSS JOIN parametros q GROUP BY e.articulo,e.inventario
            ), recetas AS (
                SELECT m.producto_id,
                    coalesce(sum(m.cantidad) FILTER(WHERE m.tipo='ENTRADA'),0) entradas,
                    coalesce(sum(m.cantidad) FILTER(WHERE m.tipo='PERDIDA'),0) perdidas,
                    coalesce(sum(m.cantidad) FILTER(WHERE m.tipo='SALIDA' AND m.observacion='Venta de producto'),0) ventas,
                    coalesce(sum(m.cantidad) FILTER(WHERE m.tipo='SALIDA' AND m.observacion IS DISTINCT FROM 'Venta de producto'),0) manuales
                FROM movimiento_inventario m,parametros q WHERE m.sede_id=q.sede
                    AND m.fecha>=q.inicio AND m.fecha<=q.corte GROUP BY m.producto_id
            )
            SELECT i.tipo,i.codigo,i.nombre,i.unidad,i.receta,
                NOT i.receta AND coalesce(t.primera<=q.inicio,false) completo,
                CASE WHEN NOT i.receta AND t.primera<=q.inicio THEN i.stock-t.cambio END inicial,
                CASE WHEN i.receta THEN coalesce(r.entradas,0) ELSE coalesce(t.entradas,0) END entradas,
                CASE WHEN i.receta THEN coalesce(r.perdidas,0) ELSE coalesce(t.perdidas,0) END perdidas,
                CASE WHEN i.receta THEN CASE WHEN lower(i.nombre) LIKE '%combo%' THEN 0 ELSE coalesce(r.ventas,0) END
                     ELSE coalesce(t.unitarias,0) END unitarias,
                CASE WHEN i.receta THEN CASE WHEN lower(i.nombre) LIKE '%combo%' THEN coalesce(r.ventas,0) ELSE 0 END
                     ELSE coalesce(t.combos,0) END combos,
                CASE WHEN i.receta THEN coalesce(r.manuales,0) ELSE coalesce(t.manuales,0) END manuales,
                CASE WHEN i.receta THEN 0 ELSE coalesce(t.ajustes,0) END ajustes,
                CASE WHEN NOT i.receta THEN i.stock END final
            FROM items i CROSS JOIN parametros q
            LEFT JOIN totales t ON t.articulo=i.tipo AND t.inventario=i.inventario
            LEFT JOIN recetas r ON i.tipo='producto' AND r.producto_id=i.codigo
            ORDER BY i.tipo,i.nombre,i.codigo
            """,(rs,n)->new Fila(rs.getString("tipo"),rs.getLong("codigo"),rs.getString("nombre"),
                    rs.getString("unidad"),rs.getBoolean("receta"),rs.getBoolean("completo"),
                    (Double)rs.getObject("inicial"),rs.getDouble("entradas"),rs.getDouble("perdidas"),
                    rs.getDouble("unitarias"),rs.getDouble("combos"),rs.getDouble("manuales"),
                    rs.getDouble("ajustes"),(Double)rs.getObject("final")),sede,corte.toLocalDate().atStartOfDay(),corte);
        return new Resumen(corte.toLocalDate(),corte,filas);
    }
}
