package proyecto.dian.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;

@Service
public class DianQrCodeService {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ssXXX");

    public String build(Data data) {
        if (data == null) throw new IllegalArgumentException("Los datos QR son obligatorios");
        return "NumFac: " + required(data.fullNumber(), "número") + "\n"
                + "FecFac: " + required(data.issueDate(), "fecha") + "\n"
                + "HorFac: " + time(data.issueTime()) + "\n"
                + "NitFac: " + required(data.supplierNit(), "NIT emisor") + "\n"
                + "DocAdq: " + required(data.customerIdentification(), "documento adquirente") + "\n"
                + "ValFac: " + money(data.untaxedAmount()) + "\n"
                + "ValIva: " + money(data.vatAmount()) + "\n"
                + "ValOtroIm: " + money(data.otherTaxAmount()) + "\n"
                + "ValTolFac: " + money(data.totalAmount()) + "\n"
                + "CUFE: " + required(data.cufe(), "CUFE") + "\n"
                + required(data.lookupUrl(), "URL de consulta") + data.cufe();
    }

    private String money(BigDecimal value) {
        if (value == null || value.signum() < 0) throw new IllegalArgumentException("Los valores QR deben ser no negativos");
        return value.setScale(2, RoundingMode.DOWN).toPlainString();
    }

    private String time(OffsetTime value) {
        if (value == null) throw new IllegalArgumentException("Falta hora");
        return value.format(TIME);
    }

    private String required(Object value, String field) {
        if (value == null || value.toString().isBlank()) throw new IllegalArgumentException("Falta " + field);
        return value.toString();
    }

    public record Data(String fullNumber, LocalDate issueDate, OffsetTime issueTime, String supplierNit,
                       String customerIdentification, BigDecimal untaxedAmount, BigDecimal vatAmount,
                       BigDecimal otherTaxAmount, BigDecimal totalAmount, String cufe, String lookupUrl) {}
}
