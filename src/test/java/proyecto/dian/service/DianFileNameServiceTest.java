package proyecto.dian.service;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class DianFileNameServiceTest {
    @Test
    void buildsDeterministicDianInvoiceNames() {
        var names = new DianFileNameService().invoice(902091864L, LocalDate.of(2026, 9, 4), 10);
        assertEquals("fv0902091864000260000000a.xml", names.xml());
        assertEquals("fv0902091864000260000000a.zip", names.zip());
    }

    @Test
    void rejectsConsecutiveOutsideEightHexDigits() {
        assertThrows(IllegalArgumentException.class,
                () -> new DianFileNameService().invoice(902091864L, LocalDate.now(), 0x100000000L));
    }
}
