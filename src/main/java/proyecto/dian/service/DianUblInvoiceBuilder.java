package proyecto.dian.service;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class DianUblInvoiceBuilder {
    static final String INVOICE_NS = "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2";
    static final String CAC_NS = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
    static final String CBC_NS = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ssXXX");

    public byte[] build(DianInvoiceData invoice) {
        validate(invoice);
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Document document = factory.newDocumentBuilder().newDocument();

            Element root = document.createElementNS(INVOICE_NS, "Invoice");
            root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:cac", CAC_NS);
            root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:cbc", CBC_NS);
            document.appendChild(root);

            text(document, root, CBC_NS, "cbc:UBLVersionID", "UBL 2.1");
            text(document, root, CBC_NS, "cbc:CustomizationID", "10");
            text(document, root, CBC_NS, "cbc:ProfileID", "DIAN 2.1: Factura Electrónica de Venta");
            text(document, root, CBC_NS, "cbc:ProfileExecutionID", invoice.profileExecutionId());
            text(document, root, CBC_NS, "cbc:ID", invoice.fullNumber());
            Element uuid = text(document, root, CBC_NS, "cbc:UUID", invoice.cufe());
            uuid.setAttribute("schemeID", invoice.profileExecutionId());
            uuid.setAttribute("schemeName", "CUFE-SHA384");
            text(document, root, CBC_NS, "cbc:IssueDate", invoice.issueDate().toString());
            text(document, root, CBC_NS, "cbc:IssueTime", invoice.issueTime().format(TIME_FORMAT));
            text(document, root, CBC_NS, "cbc:InvoiceTypeCode", "01");
            text(document, root, CBC_NS, "cbc:DocumentCurrencyCode", invoice.currencyCode());
            text(document, root, CBC_NS, "cbc:LineCountNumeric", Integer.toString(invoice.lines().size()));

            party(document, root, "cac:AccountingSupplierParty", invoice.supplier());
            party(document, root, "cac:AccountingCustomerParty", invoice.customer());
            paymentMeans(document, root, invoice);
            for (DianInvoiceData.Tax tax : invoice.taxes()) {
                taxTotal(document, root, tax, invoice.currencyCode());
            }
            monetaryTotal(document, root, invoice);
            for (DianInvoiceData.Line line : invoice.lines()) {
                invoiceLine(document, root, line, invoice.currencyCode());
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            var transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            transformer.transform(new DOMSource(document), new StreamResult(output));
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("No fue posible construir el XML UBL de la factura", exception);
        }
    }

    private void party(Document document, Element root, String elementName, DianInvoiceData.Party party) {
        Element wrapper = element(document, root, CAC_NS, elementName);
        text(document, wrapper, CBC_NS, "cbc:AdditionalAccountID", party.organizationTypeCode());
        Element partyElement = element(document, wrapper, CAC_NS, "cac:Party");
        Element physicalLocation = element(document, partyElement, CAC_NS, "cac:PhysicalLocation");
        address(document, physicalLocation, party);
        Element taxScheme = element(document, partyElement, CAC_NS, "cac:PartyTaxScheme");
        text(document, taxScheme, CBC_NS, "cbc:RegistrationName", party.registrationName());
        Element companyId = text(document, taxScheme, CBC_NS, "cbc:CompanyID", party.identification());
        companyId.setAttribute("schemeID", optional(party.verificationDigit()));
        companyId.setAttribute("schemeName", party.documentTypeCode());
        text(document, taxScheme, CBC_NS, "cbc:TaxLevelCode", party.taxLevelCode());
        address(document, taxScheme, party);
        Element tax = element(document, taxScheme, CAC_NS, "cac:TaxScheme");
        text(document, tax, CBC_NS, "cbc:ID", party.taxCode());
        text(document, tax, CBC_NS, "cbc:Name", party.taxName());
        Element legal = element(document, partyElement, CAC_NS, "cac:PartyLegalEntity");
        text(document, legal, CBC_NS, "cbc:RegistrationName", party.registrationName());
        Element legalId = text(document, legal, CBC_NS, "cbc:CompanyID", party.identification());
        legalId.setAttribute("schemeID", optional(party.verificationDigit()));
        legalId.setAttribute("schemeName", party.documentTypeCode());
        Element contact = element(document, partyElement, CAC_NS, "cac:Contact");
        text(document, contact, CBC_NS, "cbc:ElectronicMail", party.email());
    }

    private void address(Document document, Element parent, DianInvoiceData.Party party) {
        Element address = element(document, parent, CAC_NS, "cac:Address");
        text(document, address, CBC_NS, "cbc:ID", party.cityCode());
        text(document, address, CBC_NS, "cbc:CityName", party.cityName());
        text(document, address, CBC_NS, "cbc:PostalZone", party.postalCode());
        text(document, address, CBC_NS, "cbc:CountrySubentity", party.departmentName());
        text(document, address, CBC_NS, "cbc:CountrySubentityCode", party.departmentCode());
        Element line = element(document, address, CAC_NS, "cac:AddressLine");
        text(document, line, CBC_NS, "cbc:Line", party.address());
        Element country = element(document, address, CAC_NS, "cac:Country");
        text(document, country, CBC_NS, "cbc:IdentificationCode", party.countryCode());
        text(document, country, CBC_NS, "cbc:Name", party.countryName());
    }

    private void paymentMeans(Document document, Element root, DianInvoiceData invoice) {
        Element payment = element(document, root, CAC_NS, "cac:PaymentMeans");
        text(document, payment, CBC_NS, "cbc:ID", "1");
        text(document, payment, CBC_NS, "cbc:PaymentMeansCode", invoice.paymentMeansCode());
    }

    private void taxTotal(Document document, Element parent, DianInvoiceData.Tax tax, String currency) {
        Element total = element(document, parent, CAC_NS, "cac:TaxTotal");
        amount(document, total, "cbc:TaxAmount", tax.amount(), currency);
        Element subtotal = element(document, total, CAC_NS, "cac:TaxSubtotal");
        amount(document, subtotal, "cbc:TaxableAmount", tax.taxableAmount(), currency);
        amount(document, subtotal, "cbc:TaxAmount", tax.amount(), currency);
        Element category = element(document, subtotal, CAC_NS, "cac:TaxCategory");
        text(document, category, CBC_NS, "cbc:Percent", tax.percent().stripTrailingZeros().toPlainString());
        Element scheme = element(document, category, CAC_NS, "cac:TaxScheme");
        text(document, scheme, CBC_NS, "cbc:ID", tax.code());
        text(document, scheme, CBC_NS, "cbc:Name", tax.name());
    }

    private void monetaryTotal(Document document, Element root, DianInvoiceData invoice) {
        Element total = element(document, root, CAC_NS, "cac:LegalMonetaryTotal");
        amount(document, total, "cbc:LineExtensionAmount", invoice.lineExtensionAmount(), invoice.currencyCode());
        amount(document, total, "cbc:TaxExclusiveAmount", invoice.taxExclusiveAmount(), invoice.currencyCode());
        amount(document, total, "cbc:TaxInclusiveAmount", invoice.taxInclusiveAmount(), invoice.currencyCode());
        amount(document, total, "cbc:AllowanceTotalAmount", invoice.allowanceTotalAmount(), invoice.currencyCode());
        amount(document, total, "cbc:PayableAmount", invoice.payableAmount(), invoice.currencyCode());
    }

    private void invoiceLine(Document document, Element root, DianInvoiceData.Line line, String currency) {
        Element invoiceLine = element(document, root, CAC_NS, "cac:InvoiceLine");
        text(document, invoiceLine, CBC_NS, "cbc:ID", Integer.toString(line.id()));
        Element quantity = text(document, invoiceLine, CBC_NS, "cbc:InvoicedQuantity", decimal(line.quantity()));
        quantity.setAttribute("unitCode", line.unitCode());
        amount(document, invoiceLine, "cbc:LineExtensionAmount", line.lineExtensionAmount(), currency);
        for (DianInvoiceData.Tax tax : line.taxes()) {
            taxTotal(document, invoiceLine, tax, currency);
        }
        Element item = element(document, invoiceLine, CAC_NS, "cac:Item");
        text(document, item, CBC_NS, "cbc:Description", line.description());
        Element standard = element(document, item, CAC_NS, "cac:StandardItemIdentification");
        text(document, standard, CBC_NS, "cbc:ID", line.standardCode());
        Element price = element(document, invoiceLine, CAC_NS, "cac:Price");
        amount(document, price, "cbc:PriceAmount", line.unitPrice(), currency);
        Element base = text(document, price, CBC_NS, "cbc:BaseQuantity", decimal(line.baseQuantity()));
        base.setAttribute("unitCode", line.unitCode());
    }

    private Element amount(Document document, Element parent, String name, BigDecimal value, String currency) {
        Element element = text(document, parent, CBC_NS, name, money(value));
        element.setAttribute("currencyID", currency);
        return element;
    }

    private Element element(Document document, Element parent, String namespace, String name) {
        Element element = document.createElementNS(namespace, name);
        parent.appendChild(element);
        return element;
    }

    private Element text(Document document, Element parent, String namespace, String name, String value) {
        Element element = element(document, parent, namespace, name);
        element.setTextContent(value);
        return element;
    }

    private String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String decimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private String optional(String value) {
        return value == null ? "" : value;
    }

    private void validate(DianInvoiceData invoice) {
        if (invoice == null) throw new IllegalArgumentException("La factura es obligatoria");
        required(invoice.profileExecutionId(), "ambiente");
        if (!List.of("1", "2").contains(invoice.profileExecutionId())) {
            throw new IllegalArgumentException("El ambiente DIAN debe ser 1 o 2");
        }
        required(invoice.fullNumber(), "número de factura");
        required(invoice.cufe(), "CUFE");
        required(invoice.currencyCode(), "moneda");
        required(invoice.paymentMeansCode(), "medio de pago");
        if (invoice.issueDate() == null || invoice.issueTime() == null) {
            throw new IllegalArgumentException("La fecha y hora de emisión son obligatorias");
        }
        validateParty(invoice.supplier(), "emisor");
        validateParty(invoice.customer(), "adquirente");
        if (invoice.lines() == null || invoice.lines().isEmpty()) {
            throw new IllegalArgumentException("La factura debe contener líneas");
        }
        requireAmounts(invoice.lineExtensionAmount(), invoice.taxExclusiveAmount(), invoice.taxInclusiveAmount(),
                invoice.allowanceTotalAmount(), invoice.payableAmount());
        if (invoice.taxes() == null) throw new IllegalArgumentException("Los impuestos son obligatorios");
        for (DianInvoiceData.Line line : invoice.lines()) {
            if (line == null || line.id() <= 0 || line.quantity() == null || line.quantity().signum() <= 0) {
                throw new IllegalArgumentException("Cada línea debe tener identificador y cantidad positiva");
            }
            required(line.unitCode(), "unidad de medida");
            required(line.description(), "descripción del producto");
            required(line.standardCode(), "código estándar del producto");
            requireAmounts(line.lineExtensionAmount(), line.unitPrice(), line.baseQuantity());
            if (line.taxes() == null) throw new IllegalArgumentException("Los impuestos de línea son obligatorios");
        }
    }

    private void validateParty(DianInvoiceData.Party party, String label) {
        if (party == null) throw new IllegalArgumentException("El " + label + " es obligatorio");
        required(party.identification(), "identificación del " + label);
        required(party.documentTypeCode(), "tipo de documento del " + label);
        required(party.organizationTypeCode(), "tipo de organización del " + label);
        required(party.taxLevelCode(), "responsabilidad fiscal del " + label);
        required(party.registrationName(), "nombre fiscal del " + label);
        required(party.address(), "dirección del " + label);
        required(party.cityCode(), "municipio del " + label);
        required(party.countryCode(), "país del " + label);
        required(party.taxCode(), "tributo del " + label);
        required(party.taxName(), "nombre del tributo del " + label);
        required(party.email(), "correo del " + label);
    }

    private void required(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Falta " + label);
    }

    private void requireAmounts(BigDecimal... values) {
        for (BigDecimal value : values) {
            if (value == null || value.signum() < 0) {
                throw new IllegalArgumentException("Los importes deben ser valores no negativos");
            }
        }
    }
}
