package proyecto.dian.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;

@Service
public class DianCufeService {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ssXXX");
    private final DianSha384Service sha384;

    public DianCufeService(DianSha384Service sha384) {
        this.sha384 = sha384;
    }

    public String calculateCufe(Input input, String technicalKey) {
        return calculate(input, require(technicalKey, "clave técnica"));
    }

    public String calculateCude(Input input, String softwarePin) {
        return calculate(input, require(softwarePin, "Software PIN"));
    }

    private String calculate(Input input, String secret) {
        if (input == null) {
            throw new IllegalArgumentException("Los datos del documento son obligatorios");
        }
        String seed = require(input.fullNumber(), "número completo")
                + date(input.issueDate())
                + time(input.issueTime())
                + amount(input.untaxedAmount(), "valor sin impuestos")
                + "01" + amount(input.vatAmount(), "IVA")
                + "04" + amount(input.consumptionTaxAmount(), "INC")
                + "03" + amount(input.icaAmount(), "ICA")
                + amount(input.payableAmount(), "total a pagar")
                + require(input.supplierNit(), "NIT del emisor")
                + require(input.customerIdentification(), "identificación del adquirente")
                + secret
                + require(input.environmentCode(), "código de ambiente");
        return sha384.hash(seed);
    }

    private String amount(BigDecimal value, String field) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(field + " debe ser un valor no negativo");
        }
        return value.setScale(2, RoundingMode.DOWN).toPlainString();
    }

    private String date(LocalDate value) {
        return require(value, "fecha de emisión");
    }

    private String time(OffsetTime value) {
        if (value == null) {
            throw new IllegalArgumentException("hora de emisión es obligatorio");
        }
        return value.format(TIME_FORMAT);
    }

    private String require(Object value, String field) {
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
        return value.toString();
    }

    public record Input(
            String fullNumber,
            LocalDate issueDate,
            OffsetTime issueTime,
            BigDecimal untaxedAmount,
            BigDecimal vatAmount,
            BigDecimal consumptionTaxAmount,
            BigDecimal icaAmount,
            BigDecimal payableAmount,
            String supplierNit,
            String customerIdentification,
            String environmentCode
    ) {}
}
