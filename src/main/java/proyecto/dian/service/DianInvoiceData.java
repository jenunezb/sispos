package proyecto.dian.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetTime;
import java.util.List;

public record DianInvoiceData(
        String profileExecutionId,
        String fullNumber,
        String cufe,
        LocalDate issueDate,
        OffsetTime issueTime,
        String currencyCode,
        Party supplier,
        Party customer,
        String paymentMeansCode,
        BigDecimal lineExtensionAmount,
        BigDecimal taxExclusiveAmount,
        BigDecimal taxInclusiveAmount,
        BigDecimal allowanceTotalAmount,
        BigDecimal payableAmount,
        List<Tax> taxes,
        List<Line> lines
) {
    public record Party(
            String identification,
            String verificationDigit,
            String documentTypeCode,
            String organizationTypeCode,
            String taxLevelCode,
            String registrationName,
            String address,
            String cityCode,
            String cityName,
            String departmentCode,
            String departmentName,
            String postalCode,
            String countryCode,
            String countryName,
            String taxCode,
            String taxName,
            String email
    ) {}

    public record Tax(
            String code,
            String name,
            BigDecimal taxableAmount,
            BigDecimal amount,
            BigDecimal percent
    ) {}

    public record Line(
            int id,
            BigDecimal quantity,
            String unitCode,
            BigDecimal lineExtensionAmount,
            String description,
            String standardCode,
            BigDecimal unitPrice,
            BigDecimal baseQuantity,
            List<Tax> taxes
    ) {}
}
