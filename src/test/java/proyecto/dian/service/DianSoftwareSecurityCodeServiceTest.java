package proyecto.dian.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DianSoftwareSecurityCodeServiceTest {
    private final DianSoftwareSecurityCodeService service =
            new DianSoftwareSecurityCodeService(new DianSha384Service());

    @Test
    void calculatesSha384UsingExactDianConcatenationOrder() {
        assertEquals(
                "eb5eed435676575bc8834dfad08d8b472feace84c1ab94bb4dcd1241ee917d8b822be8dc2f119a4f125d66d797882655",
                service.calculate("abc", "123", "INV1")
        );
    }

    @Test
    void rejectsMissingPrivateValues() {
        assertThrows(IllegalArgumentException.class, () -> service.calculate("software", "", "SETP1"));
    }
}
