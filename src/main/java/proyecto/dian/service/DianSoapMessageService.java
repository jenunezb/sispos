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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class DianSoapMessageService {
    static final String SOAP_NS = "http://www.w3.org/2003/05/soap-envelope";
    static final String DIAN_NS = "http://wcf.dian.colombia";

    public byte[] sendTestSetAsync(String zipFileName, byte[] zip, String testSetId) {
        required(zipFileName, "nombre del ZIP");
        required(testSetId, "TestSetId");
        if (!zipFileName.matches("[A-Za-z0-9_-]+\\.zip")) {
            throw new IllegalArgumentException("El nombre del ZIP DIAN no es válido");
        }
        if (zip == null || zip.length == 0) throw new IllegalArgumentException("El ZIP DIAN está vacío");
        try {
            Document document = newDocument();
            Element envelope = document.createElementNS(SOAP_NS, "soap:Envelope");
            envelope.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:wcf", DIAN_NS);
            document.appendChild(envelope);
            envelope.appendChild(document.createElementNS(SOAP_NS, "soap:Header"));
            Element body = document.createElementNS(SOAP_NS, "soap:Body");
            envelope.appendChild(body);
            Element operation = document.createElementNS(DIAN_NS, "wcf:SendTestSetAsync");
            body.appendChild(operation);
            text(document, operation, "wcf:fileName", zipFileName);
            text(document, operation, "wcf:contentFile", Base64.getEncoder().encodeToString(zip));
            text(document, operation, "wcf:testSetId", testSetId);
            return serialize(document);
        } catch (Exception exception) {
            throw new IllegalStateException("No fue posible construir la solicitud SOAP DIAN", exception);
        }
    }

    public byte[] getStatusZip(String trackId) {
        required(trackId, "TrackId");
        try {
            Document document = newDocument();
            Element envelope = document.createElementNS(SOAP_NS, "soap:Envelope");
            envelope.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:wcf", DIAN_NS);
            document.appendChild(envelope);
            envelope.appendChild(document.createElementNS(SOAP_NS, "soap:Header"));
            Element body = document.createElementNS(SOAP_NS, "soap:Body");
            envelope.appendChild(body);
            Element operation = document.createElementNS(DIAN_NS, "wcf:GetStatusZip");
            body.appendChild(operation);
            text(document, operation, "wcf:trackId", trackId.trim());
            return serialize(document);
        } catch (Exception exception) {
            throw new IllegalStateException("No fue posible construir la consulta SOAP DIAN", exception);
        }
    }

    public DianSoapResult parseResponse(byte[] soapXml) {
        if (soapXml == null || soapXml.length == 0) {
            throw new IllegalArgumentException("La respuesta SOAP DIAN está vacía");
        }
        try {
            Document document = parse(soapXml);
            String fault = first(document, "Text");
            if (document.getElementsByTagNameNS(SOAP_NS, "Fault").getLength() > 0) {
                return new DianSoapResult(null, false, null, clean(fault), List.of());
            }
            String zipKey = first(document, "ZipKey");
            String statusCode = first(document, "StatusCode");
            String description = first(document, "StatusDescription");
            String validText = first(document, "IsValid");
            List<String> errors = all(document, "string");
            boolean valid = "true".equalsIgnoreCase(validText);
            return new DianSoapResult(clean(zipKey), valid, clean(statusCode), clean(description), errors);
        } catch (Exception exception) {
            throw new IllegalArgumentException("La respuesta de la DIAN no es un SOAP válido", exception);
        }
    }

    private Document newDocument() throws Exception {
        return secureFactory().newDocumentBuilder().newDocument();
    }

    private Document parse(byte[] xml) throws Exception {
        return secureFactory().newDocumentBuilder().parse(new ByteArrayInputStream(xml));
    }

    private DocumentBuilderFactory secureFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory;
    }

    private byte[] serialize(Document document) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        var transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(document), new StreamResult(output));
        return output.toByteArray();
    }

    private void text(Document document, Element parent, String name, String value) {
        Element child = document.createElementNS(DIAN_NS, name);
        child.setTextContent(value);
        parent.appendChild(child);
    }

    private String first(Document document, String localName) {
        var nodes = document.getElementsByTagNameNS("*", localName);
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent();
    }

    private List<String> all(Document document, String localName) {
        var nodes = document.getElementsByTagNameNS("*", localName);
        List<String> values = new ArrayList<>();
        for (int index = 0; index < nodes.getLength(); index++) {
            String value = clean(nodes.item(index).getTextContent());
            if (value != null) values.add(value);
        }
        return List.copyOf(values);
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private void required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Falta " + name);
    }

    public record DianSoapResult(String zipKey, boolean valid, String statusCode,
                                 String statusDescription, List<String> errors) {}
}
