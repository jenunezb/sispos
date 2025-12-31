package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import proyecto.dto.InventarioFinalProjection;
import proyecto.entidades.Administrador;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdministradorRepository extends JpaRepository<Administrador, Long> {

    @Query(value = "SELECT\n" +
            "    :sedeId AS sedeId,\n" +
            "    p.nombre AS productoNombre,\n" +
            "\n" +
            "    /* Inventario inicial antes del período */\n" +
            "    COALESCE(SUM(\n" +
            "        CASE\n" +
            "            WHEN m.fecha < :fechaInicio AND m.tipo = 'ENTRADA' THEN m.cantidad\n" +
            "            WHEN m.fecha < :fechaInicio AND m.tipo IN ('SALIDA','PERDIDA') THEN -m.cantidad\n" +
            "            ELSE 0\n" +
            "        END\n" +
            "    ), 0) AS inventarioInicial,\n" +
            "\n" +
            "    /* Entradas del período */\n" +
            "    COALESCE(SUM(\n" +
            "        CASE\n" +
            "            WHEN m.fecha BETWEEN :fechaInicio AND :fechaFin\n" +
            "                 AND m.tipo = 'ENTRADA'\n" +
            "            THEN m.cantidad\n" +
            "            ELSE 0\n" +
            "        END\n" +
            "    ), 0) AS entradas,\n" +
            "\n" +
            "    /* Total disponible */\n" +
            "    (\n" +
            "        COALESCE(SUM(\n" +
            "            CASE\n" +
            "                WHEN m.fecha < :fechaInicio AND m.tipo = 'ENTRADA' THEN m.cantidad\n" +
            "                WHEN m.fecha < :fechaInicio AND m.tipo IN ('SALIDA','PERDIDA') THEN -m.cantidad\n" +
            "                ELSE 0\n" +
            "            END\n" +
            "        ), 0)\n" +
            "        +\n" +
            "        COALESCE(SUM(\n" +
            "            CASE\n" +
            "                WHEN m.fecha BETWEEN :fechaInicio AND :fechaFin\n" +
            "                     AND m.tipo = 'ENTRADA'\n" +
            "                THEN m.cantidad\n" +
            "                ELSE 0\n" +
            "            END\n" +
            "        ), 0)\n" +
            "    ) AS total,\n" +
            "\n" +
            "    /* Cantidad vendida (SALIDAS del período) */\n" +
            "    COALESCE(SUM(\n" +
            "        CASE\n" +
            "            WHEN m.fecha BETWEEN :fechaInicio AND :fechaFin\n" +
            "                 AND m.tipo = 'SALIDA'\n" +
            "            THEN m.cantidad\n" +
            "            ELSE 0\n" +
            "        END\n" +
            "    ), 0) AS cantVendida,\n" +
            "\n" +
            "    /* Inventario final */\n" +
            "    (\n" +
            "        (\n" +
            "            COALESCE(SUM(\n" +
            "                CASE\n" +
            "                    WHEN m.fecha < :fechaInicio AND m.tipo = 'ENTRADA' THEN m.cantidad\n" +
            "                    WHEN m.fecha < :fechaInicio AND m.tipo IN ('SALIDA','PERDIDA') THEN -m.cantidad\n" +
            "                    ELSE 0\n" +
            "                END\n" +
            "            ), 0)\n" +
            "            +\n" +
            "            COALESCE(SUM(\n" +
            "                CASE\n" +
            "                    WHEN m.fecha BETWEEN :fechaInicio AND :fechaFin\n" +
            "                         AND m.tipo = 'ENTRADA'\n" +
            "                    THEN m.cantidad\n" +
            "                    ELSE 0\n" +
            "                END\n" +
            "            ), 0)\n" +
            "        )\n" +
            "        -\n" +
            "        COALESCE(SUM(\n" +
            "            CASE\n" +
            "                WHEN m.fecha BETWEEN :fechaInicio AND :fechaFin\n" +
            "                     AND m.tipo IN ('SALIDA','PERDIDA')\n" +
            "                THEN m.cantidad\n" +
            "                ELSE 0\n" +
            "            END\n" +
            "        ), 0)\n" +
            "    ) AS inventarioFinal,\n" +
            "\n" +
            "    /* Precio de venta del producto */\n" +
            "    p.precio_venta AS precio,\n" +
            "\n" +
            "    /* Total vendido */\n" +
            "    (\n" +
            "        COALESCE(SUM(\n" +
            "            CASE\n" +
            "                WHEN m.fecha BETWEEN :fechaInicio AND :fechaFin\n" +
            "                     AND m.tipo = 'SALIDA'\n" +
            "                THEN m.cantidad\n" +
            "                ELSE 0\n" +
            "            END\n" +
            "        ), 0) * p.precio_venta\n" +
            "    ) AS totalVendido\n" +
            "\n" +
            "FROM producto p\n" +
            "LEFT JOIN movimiento_inventario m\n" +
            "       ON m.producto_id = p.codigo\n" +
            "      AND m.sede_id = :sedeId\n" +
            "\n" +
            "GROUP BY p.codigo, p.nombre, p.precio_venta;\n", nativeQuery = true)
    List<InventarioFinalProjection> obtenerInventarioFinal(
            @Param("sedeId") Long sedeId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin
    );

}
