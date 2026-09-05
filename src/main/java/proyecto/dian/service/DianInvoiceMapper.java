package proyecto.dian.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import proyecto.dian.model.DianEnvironment;
import proyecto.entidades.*;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DianInvoiceMapper {
    private static final ZoneOffset COLOMBIA_OFFSET = ZoneOffset.ofHours(-5);
    private final DianTaxCalculator taxes;
    private final DianSoftwareSecurityCodeService securityCodes;
    private final DianCufeService cufeService;
    private final DianQrCodeService qrCodes;

    public DianInvoiceData map(Venta sale, DianEnvironment environment,
                               DianNumberingService.AllocatedNumber number,
                               String softwareId, String softwarePin, String technicalKey) {
        Empresa company = sale.getSede().getEmpresa();
        List<DianInvoiceData.Line> lines = new ArrayList<>();
        Map<String, TaxAccumulator> totalsByTax = new LinkedHashMap<>();
        BigDecimal untaxed = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;

        int lineId = 1;
        for (DetalleVenta detail : sale.getDetalles()) {
            Producto product = detail.getProducto();
            List<DianTaxCalculator.TaxRate> rates = taxRates(product);
            DianTaxCalculator.Result result = taxes.calculate(new DianTaxCalculator.LineInput(
                    BigDecimal.valueOf(detail.getCantidad()), detail.getPrecioUnitarioFiscal(),
                    detail.getDescuentoFiscal(), product.getPrecioIncluyeImpuestos(), rates));
            verifyPersistedDetail(detail, result);
            List<DianInvoiceData.Tax> lineTaxes = result.taxes().stream()
                    .map(tax -> new DianInvoiceData.Tax(tax.code(), tax.name(), tax.taxableAmount(), tax.amount(), tax.percent()))
                    .toList();
            lines.add(new DianInvoiceData.Line(lineId++, BigDecimal.valueOf(detail.getCantidad()),
                    product.getUnidadMedidaDian(), result.lineExtensionAmount(), product.getNombre(),
                    product.getCodigoEstandarFiscal(), detail.getPrecioUnitarioFiscal(), BigDecimal.ONE, lineTaxes));
            untaxed = untaxed.add(result.lineExtensionAmount());
            totalTax = totalTax.add(result.totalTax());
            totalDiscount = totalDiscount.add(result.discountAmount());
            for (DianTaxCalculator.TaxAmount tax : result.taxes()) {
                totalsByTax.computeIfAbsent(tax.code(), ignored -> new TaxAccumulator(tax.name(), tax.percent()))
                        .add(tax.taxableAmount(), tax.amount());
            }
        }
        BigDecimal payable = untaxed.add(totalTax).setScale(2);
        verifyPersistedSale(sale, untaxed, totalTax, totalDiscount, payable);
        var issued = sale.getFecha().atOffset(COLOMBIA_OFFSET);
        String environmentCode = environment == DianEnvironment.PRODUCCION ? "1" : "2";
        String securityCode = securityCodes.calculate(softwareId, softwarePin, number.fullNumber());
        BigDecimal iva = taxAmount(totalsByTax, "01");
        BigDecimal inc = taxAmount(totalsByTax, "04");
        BigDecimal ica = taxAmount(totalsByTax, "03");
        String cufe = cufeService.calculateCufe(new DianCufeService.Input(number.fullNumber(), issued.toLocalDate(),
                issued.toOffsetTime(), untaxed, iva, inc, ica, payable, company.getNit().toString(),
                sale.getCliente().getDocumento(), environmentCode), technicalKey);
        String lookup = environment == DianEnvironment.PRODUCCION
                ? "https://catalogo-vpfe.dian.gov.co/document/searchqr?documentkey="
                : "https://catalogo-vpfe-hab.dian.gov.co/document/searchqr?documentkey=";
        String qr = qrCodes.build(new DianQrCodeService.Data(number.fullNumber(), issued.toLocalDate(),
                issued.toOffsetTime(), company.getNit().toString(), sale.getCliente().getDocumento(), untaxed,
                iva, inc.add(ica), payable, cufe, lookup));
        List<DianInvoiceData.Tax> documentTaxes = totalsByTax.entrySet().stream()
                .map(entry -> entry.getValue().toTax(entry.getKey())).toList();
        return new DianInvoiceData(new DianInvoiceData.DianExtension(number.resolutionNumber(), number.validFrom(),
                number.validUntil(), number.prefix(), number.rangeFrom(), number.rangeTo(), softwareId, securityCode, qr),
                environmentCode, number.fullNumber(), cufe, issued.toLocalDate(), issued.toOffsetTime(),
                sale.getMonedaCodigo(), party(company, sale.getSede()), party(sale.getCliente()), sale.getFormaPagoDian(),
                sale.getMedioPagoDian(), sale.getFechaVencimientoPago() == null
                        ? null : sale.getFechaVencimientoPago().toLocalDate(),
                untaxed, untaxed, payable, totalDiscount, payable, documentTaxes, lines);
    }

    private List<DianTaxCalculator.TaxRate> taxRates(Producto product) {
        List<DianTaxCalculator.TaxRate> result = new ArrayList<>();
        addRate(result, "01", "IVA", product.getTarifaIva());
        addRate(result, "04", "INC", product.getTarifaInc());
        addRate(result, "03", "ICA", product.getTarifaIca());
        return result;
    }

    private void addRate(List<DianTaxCalculator.TaxRate> rates, String code, String name, BigDecimal percent) {
        if (percent != null && percent.signum() >= 0) rates.add(new DianTaxCalculator.TaxRate(code, name, percent));
    }

    private void verifyPersistedDetail(DetalleVenta detail, DianTaxCalculator.Result result) {
        equalIfPresent(detail.getSubtotalFiscal(), result.lineExtensionAmount(), "subtotal fiscal de línea");
        equalIfPresent(detail.getBaseImpuestoFiscal(), result.lineExtensionAmount(), "base fiscal de línea");
        equalIfPresent(detail.getValorImpuestoFiscal(), result.totalTax(), "impuesto fiscal de línea");
    }

    private void verifyPersistedSale(Venta sale, BigDecimal subtotal, BigDecimal tax, BigDecimal discount, BigDecimal total) {
        equalIfPresent(sale.getSubtotalFiscal(), subtotal, "subtotal fiscal de venta");
        equalIfPresent(sale.getImpuestosFiscal(), tax, "impuestos fiscales de venta");
        equalIfPresent(sale.getDescuentosFiscal(), discount, "descuentos fiscales de venta");
        equalIfPresent(sale.getTotalFiscal(), total, "total fiscal de venta");
    }

    private void equalIfPresent(BigDecimal persisted, BigDecimal calculated, String field) {
        if (persisted != null && persisted.setScale(2, java.math.RoundingMode.HALF_UP).compareTo(calculated) != 0)
            throw new IllegalArgumentException("El " + field + " no coincide con el cálculo del backend");
    }

    private BigDecimal taxAmount(Map<String, TaxAccumulator> taxes, String code) {
        return taxes.containsKey(code) ? taxes.get(code).amount : BigDecimal.ZERO.setScale(2);
    }

    private DianInvoiceData.Party party(Empresa company, Sede branch) {
        return new DianInvoiceData.Party(company.getNit().toString(), company.getDv(), company.getTipoDocumentoFiscal(),
                company.getTipoPersonaFiscal(), company.getResponsabilidadFiscal(), first(company.getRazonSocial(), company.getNombre()),
                first(branch.getDireccionFiscal(), company.getDireccionFiscal()), first(branch.getMunicipioCodigo(), company.getMunicipioCodigo()),
                first(branch.getMunicipioNombre(), company.getMunicipioNombre()), first(branch.getDepartamentoCodigo(), company.getDepartamentoCodigo()),
                first(branch.getDepartamentoNombre(), company.getDepartamentoNombre()), first(branch.getCodigoPostal(), company.getCodigoPostal()),
                first(branch.getPaisCodigo(), company.getPaisCodigo()), first(branch.getPaisNombre(), company.getPaisNombre()),
                company.getTributoCodigo(), company.getTributoNombre(), company.getCorreoFacturacion());
    }

    private DianInvoiceData.Party party(Cliente customer) {
        return new DianInvoiceData.Party(customer.getDocumento(), customer.getDv(), customer.getTipoDocumentoFiscal(),
                customer.getTipoPersonaFiscal(), customer.getResponsabilidadFiscal(), customer.getNombre(), customer.getDireccion(),
                customer.getMunicipioCodigo(), customer.getMunicipioNombre(), customer.getDepartamentoCodigo(),
                customer.getDepartamentoNombre(), customer.getCodigoPostal(), customer.getPaisCodigo(), customer.getPaisNombre(),
                customer.getTributoCodigo(), customer.getTributoNombre(), customer.getCorreo());
    }

    private String first(String value, String fallback) { return value != null && !value.isBlank() ? value : fallback; }

    private static final class TaxAccumulator {
        private final String name; private final BigDecimal percent;
        private BigDecimal taxable = BigDecimal.ZERO; private BigDecimal amount = BigDecimal.ZERO;
        private TaxAccumulator(String name, BigDecimal percent) { this.name = name; this.percent = percent; }
        private void add(BigDecimal base, BigDecimal tax) { taxable = taxable.add(base); amount = amount.add(tax); }
        private DianInvoiceData.Tax toTax(String code) { return new DianInvoiceData.Tax(code, name, taxable, amount, percent); }
    }
}
