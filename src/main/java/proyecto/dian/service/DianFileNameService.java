package proyecto.dian.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Locale;

@Service
public class DianFileNameService {
    public Names invoice(Long issuerNit, LocalDate issueDate, long consecutive) {
        if (issuerNit == null || issuerNit <= 0 || issuerNit.toString().length() > 10) {
            throw new IllegalArgumentException("El NIT emisor no permite construir el nombre DIAN");
        }
        if (issueDate == null || consecutive < 0 || consecutive > 0xffffffffL) {
            throw new IllegalArgumentException("Fecha o consecutivo inválido para el nombre DIAN");
        }
        String base = "fv" + String.format(Locale.ROOT, "%010d", issuerNit) + "000"
                + String.format(Locale.ROOT, "%02d", issueDate.getYear() % 100)
                + String.format(Locale.ROOT, "%08x", consecutive);
        return new Names(base + ".xml", base + ".zip");
    }

    public record Names(String xml, String zip) {}
}
