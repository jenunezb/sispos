package proyecto.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class FechaColombiaUtils {

    public static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    private FechaColombiaUtils() {
    }

    public static LocalDateTime ahora() {
        return ZonedDateTime.now(ZONA_BOGOTA).toLocalDateTime();
    }

    public static LocalDate hoy() {
        return LocalDate.now(ZONA_BOGOTA);
    }
}
