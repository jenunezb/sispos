package proyecto.dian.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class DianTaxCalculator {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    public Result calculate(LineInput input) {
        if (input == null || input.quantity() == null || input.quantity().signum() <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser positiva");
        }
        requireNonNegative(input.unitPrice(), "precio unitario");
        BigDecimal discount = input.discount() == null ? BigDecimal.ZERO : input.discount();
        requireNonNegative(discount, "descuento");
        if (input.priceIncludesTaxes() == null) {
            throw new IllegalArgumentException("Debe indicar si el precio incluye impuestos");
        }
        List<TaxRate> rates = input.taxRates() == null ? List.of() : input.taxRates();
        for (TaxRate rate : rates) {
            if (rate == null || rate.percent() == null || rate.percent().signum() < 0) {
                throw new IllegalArgumentException("Las tarifas de impuestos deben ser no negativas");
            }
        }

        BigDecimal gross = input.unitPrice().multiply(input.quantity());
        if (discount.compareTo(gross) > 0) throw new IllegalArgumentException("El descuento supera el valor bruto");
        BigDecimal afterDiscount = gross.subtract(discount);
        BigDecimal totalRate = rates.stream().map(TaxRate::percent).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taxable = input.priceIncludesTaxes() && totalRate.signum() > 0
                ? afterDiscount.divide(BigDecimal.ONE.add(totalRate.divide(ONE_HUNDRED, 12, RoundingMode.HALF_UP)), 6, RoundingMode.HALF_UP)
                : afterDiscount;
        taxable = money(taxable);

        List<TaxAmount> taxes = new ArrayList<>();
        BigDecimal totalTax = BigDecimal.ZERO;
        for (TaxRate rate : rates) {
            BigDecimal amount = money(taxable.multiply(rate.percent()).divide(ONE_HUNDRED, 12, RoundingMode.HALF_UP));
            taxes.add(new TaxAmount(rate.code(), rate.name(), rate.percent(), taxable, amount));
            totalTax = totalTax.add(amount);
        }
        BigDecimal payable = input.priceIncludesTaxes() ? money(afterDiscount) : money(taxable.add(totalTax));
        return new Result(money(gross), money(discount), taxable, List.copyOf(taxes), money(totalTax), payable);
    }

    private void requireNonNegative(BigDecimal value, String field) {
        if (value == null || value.signum() < 0) throw new IllegalArgumentException("El " + field + " debe ser no negativo");
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public record LineInput(BigDecimal quantity, BigDecimal unitPrice, BigDecimal discount,
                            Boolean priceIncludesTaxes, List<TaxRate> taxRates) {}
    public record TaxRate(String code, String name, BigDecimal percent) {}
    public record TaxAmount(String code, String name, BigDecimal percent, BigDecimal taxableAmount, BigDecimal amount) {}
    public record Result(BigDecimal grossAmount, BigDecimal discountAmount, BigDecimal lineExtensionAmount,
                         List<TaxAmount> taxes, BigDecimal totalTax, BigDecimal payableAmount) {}
}
