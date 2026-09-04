package proyecto.dian.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DianCufeServiceTest {
    private final DianCufeService service = new DianCufeService(new DianSha384Service());

    @Test
    void matchesOfficialDianAnnex19InvoiceVector() {
        DianCufeService.Input input = new DianCufeService.Input(
                "323200000129",
                LocalDate.parse("2019-01-16"),
                OffsetTime.parse("10:53:10-05:00"),
                new BigDecimal("1500000.00"),
                new BigDecimal("285000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("1785000.00"),
                "700085371",
                "800199436",
                "1"
        );

        assertEquals(
                "8bb918b19ba22a694f1da11c643b5e9de39adf60311cf179179e9b33381030bcd4c3c3f156c506ed5908f9276f5bd9b4",
                service.calculateCufe(input, "693ff6f2a553c3646a063436fd4dd9ded0311471")
        );
    }

    @Test
    void truncatesAmountsToTwoDecimalsAsRequiredByTheAnnex() {
        DianCufeService.Input input = new DianCufeService.Input(
                "FV1", LocalDate.parse("2026-09-04"), OffsetTime.parse("23:59:59-05:00"),
                new BigDecimal("1.239"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("1.239"), "902091864", "222222222222", "2"
        );
        DianCufeService.Input truncated = new DianCufeService.Input(
                "FV1", LocalDate.parse("2026-09-04"), OffsetTime.parse("23:59:59-05:00"),
                new BigDecimal("1.23"), new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00"),
                new BigDecimal("1.23"), "902091864", "222222222222", "2"
        );

        assertEquals(service.calculateCufe(truncated, "clave"), service.calculateCufe(input, "clave"));
    }

    @Test
    void rejectsNegativeAmounts() {
        DianCufeService.Input input = new DianCufeService.Input(
                "FV1", LocalDate.now(), OffsetTime.now(), BigDecimal.ONE,
                new BigDecimal("-1"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE,
                "902091864", "222222222222", "2"
        );
        assertThrows(IllegalArgumentException.class, () -> service.calculateCufe(input, "clave"));
    }
}
