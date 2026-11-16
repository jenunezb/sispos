package proyecto.dian.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DianTaxCalculatorTest {
    private final DianTaxCalculator calculator = new DianTaxCalculator();
    private final DianTaxCalculator.TaxRate iva19 =
            new DianTaxCalculator.TaxRate("01", "IVA", new BigDecimal("19"));

    @Test
    void calculatesTaxExclusivePriceWithBigDecimal() {
        var result = calculator.calculate(new DianTaxCalculator.LineInput(
                new BigDecimal("2"), new BigDecimal("100.00"), BigDecimal.ZERO, false, List.of(iva19)));
        assertEquals(new BigDecimal("200.00"), result.lineExtensionAmount());
        assertEquals(new BigDecimal("38.00"), result.totalTax());
        assertEquals(new BigDecimal("238.00"), result.payableAmount());
    }

    @Test
    void extractsTaxFromTaxInclusivePriceWithoutDoubleCharging() {
        var result = calculator.calculate(new DianTaxCalculator.LineInput(
                BigDecimal.ONE, new BigDecimal("119.00"), BigDecimal.ZERO, true, List.of(iva19)));
        assertEquals(new BigDecimal("100.00"), result.lineExtensionAmount());
        assertEquals(new BigDecimal("19.00"), result.totalTax());
        assertEquals(new BigDecimal("119.00"), result.payableAmount());
    }

    @Test
    void refusesAmbiguousOrInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(new DianTaxCalculator.LineInput(
                BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, null, List.of())));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(new DianTaxCalculator.LineInput(
                BigDecimal.ONE, BigDecimal.TEN, new BigDecimal("11"), false, List.of())));
    }
}
