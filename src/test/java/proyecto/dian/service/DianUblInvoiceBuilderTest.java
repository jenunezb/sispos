package proyecto.dian.service;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DianUblInvoiceBuilderTest {
    private final DianUblInvoiceBuilder builder = new DianUblInvoiceBuilder();

    @Test
    void buildsUtf8NamespaceAwareUblWithoutConcatenatingUnescapedContent() throws Exception {
        byte[] xml = builder.build(invoice("Café & Pan <Especial>"));
        Document document = parse(xml);

        assertEquals(DianUblInvoiceBuilder.INVOICE_NS, document.getDocumentElement().getNamespaceURI());
        assertEquals("UBL 2.1", document.getElementsByTagNameNS(DianUblInvoiceBuilder.CBC_NS, "UBLVersionID").item(0).getTextContent());
        assertEquals("SETP1", document.getElementsByTagNameNS(DianUblInvoiceBuilder.CBC_NS, "ID").item(0).getTextContent());
        assertEquals("Café & Pan <Especial>", document.getElementsByTagNameNS(DianUblInvoiceBuilder.CBC_NS, "Description").item(0).getTextContent());
        assertTrue(new String(xml, StandardCharsets.UTF_8).contains("Café &amp; Pan &lt;Especial&gt;"));
        assertEquals(1, document.getElementsByTagNameNS(DianUblInvoiceBuilder.CAC_NS, "InvoiceLine").getLength());
    }

    @Test
    void rejectsIncompleteFiscalIdentityBeforeProducingXml() {
        DianInvoiceData valid = invoice("Producto");
        DianInvoiceData.Party invalidCustomer = new DianInvoiceData.Party(
                "222222222222", null, "13", "2", "R-99-PN", "Consumidor final",
                null, "11001", "Bogotá", "11", "Bogotá D.C.", "110111", "CO", "Colombia",
                "ZZ", "No aplica", "cliente@example.com"
        );
        DianInvoiceData invalid = new DianInvoiceData(
                valid.profileExecutionId(), valid.fullNumber(), valid.cufe(), valid.issueDate(), valid.issueTime(),
                valid.currencyCode(), valid.supplier(), invalidCustomer, valid.paymentMeansCode(),
                valid.lineExtensionAmount(), valid.taxExclusiveAmount(), valid.taxInclusiveAmount(),
                valid.allowanceTotalAmount(), valid.payableAmount(), valid.taxes(), valid.lines()
        );

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> builder.build(invalid));
        assertTrue(error.getMessage().contains("dirección"));
    }

    private Document parse(byte[] xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
    }

    static DianInvoiceData invoice(String description) {
        DianInvoiceData.Party supplier = party("902091864", "2", "JULIAN ESTEBAN NUÑEZ BEJARANO", "juesnube@gmail.com");
        DianInvoiceData.Party customer = party("222222222222", null, "Consumidor final", "cliente@example.com");
        DianInvoiceData.Tax tax = new DianInvoiceData.Tax("01", "IVA", new BigDecimal("100.00"), new BigDecimal("19.00"), new BigDecimal("19.00"));
        DianInvoiceData.Line line = new DianInvoiceData.Line(
                1, BigDecimal.ONE, "EA", new BigDecimal("100.00"), description, "001",
                new BigDecimal("100.00"), BigDecimal.ONE, List.of(tax)
        );
        return new DianInvoiceData(
                "2", "SETP1", "a".repeat(96), LocalDate.parse("2026-09-04"),
                OffsetTime.parse("20:15:00-05:00"), "COP", supplier, customer, "10",
                new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("119.00"),
                BigDecimal.ZERO, new BigDecimal("119.00"), List.of(tax), List.of(line)
        );
    }

    private static DianInvoiceData.Party party(String id, String dv, String name, String email) {
        return new DianInvoiceData.Party(
                id, dv, "31", "2", "R-99-PN", name, "Calle 1 # 2-3", "11001", "Bogotá",
                "11", "Bogotá D.C.", "110111", "CO", "Colombia", "01", "IVA", email
        );
    }
}
