package proyecto.dian.service;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

@Service
public class DianWsSecuritySigner {
    static final String ADDRESSING_NS = "http://www.w3.org/2005/08/addressing";
    static final String WSSE_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    static final String WSU_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
    static final String X509_VALUE_TYPE = WSSE_NS + "#X509v3";
    static final String BASE64_ENCODING_TYPE = WSSE_NS + "#Base64Binary";
    private static final String EXCLUSIVE_C14N = "http://www.w3.org/2001/10/xml-exc-c14n#";
    private static final String RSA_SHA256 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
    private static final String SHA256 = "http://www.w3.org/2001/04/xmlenc#sha256";

    public byte[] sign(byte[] soapEnvelope, byte[] pkcs12, String password,
                       String serviceUrl, String soapAction) {
        return sign(soapEnvelope, pkcs12, password, serviceUrl, soapAction, Instant.now());
    }

    byte[] sign(byte[] soapEnvelope, byte[] pkcs12, String password,
                String serviceUrl, String soapAction, Instant createdAt) {
        required(soapEnvelope, "sobre SOAP");
        required(pkcs12, "certificado P12/PFX");
        required(password, "contraseña del certificado");
        required(serviceUrl, "URL del servicio DIAN");
        required(soapAction, "acción SOAP DIAN");
        if (!serviceUrl.startsWith("https://")) {
            throw new IllegalArgumentException("La URL DIAN debe usar HTTPS");
        }
        try {
            Identity identity = identity(pkcs12, password);
            Document document = parse(soapEnvelope);
            Element envelope = document.getDocumentElement();
            if (!DianSoapMessageService.SOAP_NS.equals(envelope.getNamespaceURI())
                    || !"Envelope".equals(envelope.getLocalName())) {
                throw new IllegalArgumentException("El documento no es un sobre SOAP 1.2");
            }
            declare(envelope, "soap", DianSoapMessageService.SOAP_NS);
            declare(envelope, "wsa", ADDRESSING_NS);
            declare(envelope, "wsse", WSSE_NS);
            declare(envelope, "wsu", WSU_NS);
            Element header = firstChild(envelope, DianSoapMessageService.SOAP_NS, "Header");
            if (header == null) {
                header = document.createElementNS(DianSoapMessageService.SOAP_NS, "soap:Header");
                envelope.insertBefore(header, envelope.getFirstChild());
            }

            String suffix = UUID.randomUUID().toString();
            Element action = addressing(document, "Action", soapAction, "Action-" + suffix);
            Element to = addressing(document, "To", serviceUrl, "To-" + suffix);
            header.appendChild(action);
            header.appendChild(to);

            Element security = document.createElementNS(WSSE_NS, "wsse:Security");
            security.setAttributeNS(DianSoapMessageService.SOAP_NS, "soap:mustUnderstand", "true");
            header.appendChild(security);

            String timestampId = "TS-" + suffix;
            Element timestamp = document.createElementNS(WSU_NS, "wsu:Timestamp");
            setId(timestamp, timestampId);
            timestamp.appendChild(text(document, WSU_NS, "wsu:Created", format(createdAt)));
            timestamp.appendChild(text(document, WSU_NS, "wsu:Expires", format(createdAt.plus(60, ChronoUnit.SECONDS))));
            security.appendChild(timestamp);

            String tokenId = "X509-" + suffix;
            Element token = document.createElementNS(WSSE_NS, "wsse:BinarySecurityToken");
            setId(token, tokenId);
            token.setAttribute("ValueType", X509_VALUE_TYPE);
            token.setAttribute("EncodingType", BASE64_ENCODING_TYPE);
            token.setTextContent(Base64.getEncoder().encodeToString(identity.certificate().getEncoded()));
            security.appendChild(token);

            XMLSignatureFactory signatureFactory = XMLSignatureFactory.getInstance("DOM");
            var transform = signatureFactory.newTransform(EXCLUSIVE_C14N, (javax.xml.crypto.dsig.spec.TransformParameterSpec) null);
            Reference toReference = signatureFactory.newReference("#" + to.getAttributeNS(WSU_NS, "Id"),
                    signatureFactory.newDigestMethod(SHA256, null), List.of(transform), null, null);
            Reference timestampReference = signatureFactory.newReference("#" + timestampId,
                    signatureFactory.newDigestMethod(SHA256, null), List.of(transform), null, null);
            SignedInfo signedInfo = signatureFactory.newSignedInfo(
                    signatureFactory.newCanonicalizationMethod(EXCLUSIVE_C14N,
                            (javax.xml.crypto.dsig.spec.C14NMethodParameterSpec) null),
                    signatureFactory.newSignatureMethod(RSA_SHA256, null),
                    List.of(toReference, timestampReference));

            Element securityTokenReference = document.createElementNS(WSSE_NS, "wsse:SecurityTokenReference");
            Element reference = document.createElementNS(WSSE_NS, "wsse:Reference");
            reference.setAttribute("URI", "#" + tokenId);
            reference.setAttribute("ValueType", X509_VALUE_TYPE);
            securityTokenReference.appendChild(reference);
            KeyInfo keyInfo = signatureFactory.getKeyInfoFactory()
                    .newKeyInfo(List.of(new DOMStructure(securityTokenReference)));

            DOMSignContext context = new DOMSignContext(identity.privateKey(), security);
            context.setDefaultNamespacePrefix("ds");
            context.setIdAttributeNS(to, WSU_NS, "Id");
            context.setIdAttributeNS(timestamp, WSU_NS, "Id");
            signatureFactory.newXMLSignature(signedInfo, keyInfo).sign(context);
            return serialize(document);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("No fue posible proteger la solicitud SOAP para la DIAN", exception);
        }
    }

    private Identity identity(byte[] pkcs12, String password) throws Exception {
        KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(new ByteArrayInputStream(pkcs12), password.toCharArray());
        Enumeration<String> aliases = store.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (store.isKeyEntry(alias) && store.getKey(alias, password.toCharArray()) instanceof PrivateKey key
                    && store.getCertificate(alias) instanceof X509Certificate certificate) {
                certificate.checkValidity();
                return new Identity(key, certificate);
            }
        }
        throw new IllegalArgumentException("El P12/PFX no contiene una llave privada X.509 vigente");
    }

    private Element addressing(Document document, String localName, String value, String id) {
        Element element = document.createElementNS(ADDRESSING_NS, "wsa:" + localName);
        element.setAttributeNS(DianSoapMessageService.SOAP_NS, "soap:mustUnderstand", "true");
        setId(element, id);
        element.setTextContent(value.trim());
        return element;
    }

    private void setId(Element element, String id) {
        element.setAttributeNS(WSU_NS, "wsu:Id", id);
    }

    private void declare(Element element, String prefix, String namespace) {
        element.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:" + prefix, namespace);
    }

    private Element text(Document document, String namespace, String name, String value) {
        Element element = document.createElementNS(namespace, name);
        element.setTextContent(value);
        return element;
    }

    private String format(Instant value) {
        return DateTimeFormatter.ISO_INSTANT.format(value.truncatedTo(ChronoUnit.MILLIS));
    }

    private Element firstChild(Element parent, String namespace, String localName) {
        for (var child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element element && namespace.equals(element.getNamespaceURI())
                    && localName.equals(element.getLocalName())) return element;
        }
        return null;
    }

    private Document parse(byte[] xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
    }

    private byte[] serialize(Document document) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        var transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(document), new StreamResult(output));
        return output.toByteArray();
    }

    private void required(byte[] value, String name) {
        if (value == null || value.length == 0) throw new IllegalArgumentException("Falta " + name);
    }

    private void required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Falta " + name);
    }

    private record Identity(PrivateKey privateKey, X509Certificate certificate) {}
}
