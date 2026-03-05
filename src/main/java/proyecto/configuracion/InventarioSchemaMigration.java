package proyecto.configuracion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventarioSchemaMigration {

    private static final String TABLE = "inventario";

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureInventarioColumns() {
        ensureColumnExists("stock_minimo", "INTEGER NOT NULL DEFAULT 0");
        ensureColumnExists("alerta_stock_minimo_activa", "BOOLEAN NOT NULL DEFAULT FALSE");
    }

    private void ensureColumnExists(String columnName, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_name = ?
                  AND column_name = ?
                """,
                Integer.class,
                TABLE,
                columnName
        );

        if (count != null && count > 0) {
            return;
        }

        String alterSql = "ALTER TABLE " + TABLE + " ADD COLUMN " + columnName + " " + definition;
        jdbcTemplate.execute(alterSql);
        log.info("Migracion aplicada: columna {} agregada en {}", columnName, TABLE);
    }
}
