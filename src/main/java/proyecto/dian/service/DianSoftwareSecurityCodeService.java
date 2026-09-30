package proyecto.dian.service;

import org.springframework.stereotype.Service;

@Service
public class DianSoftwareSecurityCodeService {
    private final DianSha384Service sha384;

    public DianSoftwareSecurityCodeService(DianSha384Service sha384) {
        this.sha384 = sha384;
    }

    public String calculate(String softwareId, String softwarePin, String fullDocumentNumber) {
        return sha384.hash(require(softwareId, "Software ID")
                + require(softwarePin, "Software PIN")
                + require(fullDocumentNumber, "número completo del documento"));
    }

    private String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
        return value;
    }
}
