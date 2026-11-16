package proyecto.dian.service;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetTime;
import static org.junit.jupiter.api.Assertions.*;

class DianQrCodeServiceTest {
    @Test
    void buildsDianQrPayloadWithoutSecrets() {
        String cufe = "a".repeat(96);
        String value = new DianQrCodeService().build(new DianQrCodeService.Data(
                "SETP1", LocalDate.parse("2026-09-04"), OffsetTime.parse("23:10:00-05:00"),
                "902091864", "222222222222", new BigDecimal("100.009"), new BigDecimal("19"),
                BigDecimal.ZERO, new BigDecimal("119"), cufe,
                "https://catalogo-vpfe-hab.dian.gov.co/document/searchqr?documentkey="));
        assertTrue(value.contains("ValFac: 100.00"));
        assertTrue(value.endsWith(cufe));
        assertFalse(value.contains("PIN"));
    }
}
