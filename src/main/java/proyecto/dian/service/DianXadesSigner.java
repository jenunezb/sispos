package proyecto.dian.service;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DianXadesSigner {
    static final String DS_NS = XMLSignature.XMLNS;
    static final String XADES_NS = "http://uri.etsi.org/01903/v1.3.2#";
    static final String XADES_SIGNED_PROPERTIES = "http://uri.etsi.org/01903#SignedProperties";
    static final String POLICY_URL =
            "https://facturaelectronica.dian.gov.co/politicadefirma/v2/politicadefirmav2.pdf";
    static final String POLICY_SHA256 = "dMoMvtcG5aIzgYo0tIsSQeVJBDnUnfSOfBpxXrmor0Y=";
    private static final String SHA256 = "http://www.w3.org/2001/04/xmlenc#sha256";
    private static final String RSA_SHA256 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
    private static final String C14N = CanonicalizationMethod.INCLUSIVE;
    private static final ZoneOffset COLOMBIA_OFFSET = ZoneOffset.ofHours(-5);

    public byte[] sign(byte[] unsignedXml, byte[] pkcs12, String password) {
        return sign(unsignedXml, pkcs12, password, OffsetDateTime.now(COLOMBIA_OFFSET));
    }

    byte[] sign(byte[] unsignedXml, byte[] pkcs12, String password, OffsetDateTime signingTime) {
        require(unsignedXml, "El XML por firmar está vacío");
        require(pkcs12, "El certificado P12/PFX está vacío");
        if (password == null) throw new IllegalArgumentException("La contraseña del certificado es obligatoria");
        try {
            SigningIdentity identity = loadIdentity(pkcs12, password);
            Document document = parse(unsignedXml);
            Element signatureParent = appendSignatureExtension(document);
            String token = UUID.randomUUID().toString();
            String signatureId = "xmldsig-" + token;
            String keyInfoId = signatureId + "-keyinfo";
            String signedPropertiesId = signatureId + "-signedprops";

            XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM");
            DigestMethod digest = factory.newDigestMethod(SHA256, null);
            Reference documentReference = factory.newReference("", digest, List.of(
                    factory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null),
                    factory.newTransform(C14N, (TransformParameterSpec) null)), null, signatureId + "-ref0");
            Reference keyInfoReference = factory.newReference("#" + keyInfoId, digest);
            Reference propertiesReference = factory.newReference("#" + signedPropertiesId, digest,
                    null, XADES_SIGNED_PROPERTIES, null);
            SignedInfo signedInfo = factory.newSignedInfo(
                    factory.newCanonicalizationMethod(C14N, (C14NMethodParameterSpec) null),
                    factory.newSignatureMethod(RSA_SHA256, null),
                    List.of(documentReference, keyInfoReference, propertiesReference));

            KeyInfoFactory keyFactory = factory.getKeyInfoFactory();
            List<Object> certificates = new ArrayList<>(identity.chain());
            X509Data x509Data = keyFactory.newX509Data(certificates);
            KeyInfo keyInfo = keyFactory.newKeyInfo(List.of(x509Data), keyInfoId);
            Element properties = qualifyingProperties(document, signatureId, signedPropertiesId,
                    identity.chain(), signingTime.withOffsetSameInstant(COLOMBIA_OFFSET));
            XMLObject object = factory.newXMLObject(List.of(new DOMStructure(properties)), null, null, null);

            DOMSignContext context = new DOMSignContext(identity.privateKey(), signatureParent);
            context.setDefaultNamespacePrefix("ds");
            context.putNamespacePrefix(DS_NS, "ds");
            context.setIdAttributeNS(properties.getFirstChild() instanceof Element child ? child : properties,
                    null, "Id");
            XMLSignature signature = factory.newXMLSignature(signedInfo, keyInfo, List.of(object), signatureId,
                    signatureId + "-sigvalue");
            signature.sign(context);
            return serialize(document);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("No fue posible firmar el XML con el certificado DIAN", exception);
        }
    }

    private SigningIdentity loadIdentity(byte[] bytes, String password) {
        try {
            KeyStore store = KeyStore.getInstance("PKCS12");
            store.load(new ByteArrayInputStream(bytes), password.toCharArray());
            for (Enumeration<String> aliases = store.aliases(); aliases.hasMoreElements();) {
                String alias = aliases.nextElement();
                Key key = store.getKey(alias, password.toCharArray());
                if (!(key instanceof PrivateKey privateKey) || !"RSA".equalsIgnoreCase(privateKey.getAlgorithm())) continue;
                Certificate[] storedChain = store.getCertificateChain(alias);
                if (storedChain == null || storedChain.length == 0) continue;
                List<X509Certificate> chain = new ArrayList<>();
                for (Certificate certificate : storedChain) {
                    if (!(certificate instanceof X509Certificate x509)) {
                        throw new IllegalArgumentException("La cadena contiene un certificado que no es X.509");
                    }
                    x509.checkValidity();
                    chain.add(x509);
                }
                return new SigningIdentity(privateKey, List.copyOf(chain));
            }
            throw new IllegalArgumentException("El P12/PFX no contiene una llave privada RSA con certificado vigente");
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("El certificado o su contraseña no son válidos", exception);
        }
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

    private Element appendSignatureExtension(Document document) {
        var extensions = document.getElementsByTagNameNS(DianUblInvoiceBuilder.EXT_NS, "UBLExtensions");
        if (extensions.getLength() != 1) {
            throw new IllegalArgumentException("El XML no contiene una única sección UBLExtensions");
        }
        Element extension = document.createElementNS(DianUblInvoiceBuilder.EXT_NS, "ext:UBLExtension");
        Element content = document.createElementNS(DianUblInvoiceBuilder.EXT_NS, "ext:ExtensionContent");
        extension.appendChild(content);
        extensions.item(0).appendChild(extension);
        return content;
    }

    private Element qualifyingProperties(Document document, String signatureId, String propertiesId,
                                         List<X509Certificate> chain, OffsetDateTime signingTime) throws Exception {
        Element qualifying = element(document, XADES_NS, "xades:QualifyingProperties");
        qualifying.setAttribute("Target", "#" + signatureId);
        Element signedProperties = child(document, qualifying, XADES_NS, "xades:SignedProperties");
        signedProperties.setAttribute("Id", propertiesId);
        signedProperties.setIdAttribute("Id", true);
        Element signatureProperties = child(document, signedProperties, XADES_NS, "xades:SignedSignatureProperties");
        text(document, signatureProperties, XADES_NS, "xades:SigningTime",
                signingTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        Element signingCertificate = child(document, signatureProperties, XADES_NS, "xades:SigningCertificate");
        for (X509Certificate certificate : chain) appendCertificate(document, signingCertificate, certificate);
        appendPolicy(document, signatureProperties);
        Element signerRole = child(document, signatureProperties, XADES_NS, "xades:SignerRole");
        Element claimedRoles = child(document, signerRole, XADES_NS, "xades:ClaimedRoles");
        text(document, claimedRoles, XADES_NS, "xades:ClaimedRole", "supplier");
        return qualifying;
    }

    private void appendCertificate(Document document, Element parent, X509Certificate certificate) throws Exception {
        Element cert = child(document, parent, XADES_NS, "xades:Cert");
        Element digest = child(document, cert, XADES_NS, "xades:CertDigest");
        Element method = child(document, digest, DS_NS, "ds:DigestMethod");
        method.setAttribute("Algorithm", SHA256);
        text(document, digest, DS_NS, "ds:DigestValue", Base64.getEncoder().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded())));
        Element issuer = child(document, cert, XADES_NS, "xades:IssuerSerial");
        text(document, issuer, DS_NS, "ds:X509IssuerName", certificate.getIssuerX500Principal().getName());
        text(document, issuer, DS_NS, "ds:X509SerialNumber", certificate.getSerialNumber().toString());
    }

    private void appendPolicy(Document document, Element parent) {
        Element identifier = child(document, parent, XADES_NS, "xades:SignaturePolicyIdentifier");
        Element policyId = child(document, identifier, XADES_NS, "xades:SignaturePolicyId");
        Element sigPolicyId = child(document, policyId, XADES_NS, "xades:SigPolicyId");
        text(document, sigPolicyId, XADES_NS, "xades:Identifier", POLICY_URL);
        text(document, sigPolicyId, XADES_NS, "xades:Description",
                "Política de firma para facturas electrónicas de la República de Colombia");
        Element policyHash = child(document, policyId, XADES_NS, "xades:SigPolicyHash");
        Element method = child(document, policyHash, DS_NS, "ds:DigestMethod");
        method.setAttribute("Algorithm", SHA256);
        text(document, policyHash, DS_NS, "ds:DigestValue", POLICY_SHA256);
    }

    private byte[] serialize(Document document) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        var transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(document), new StreamResult(output));
        return output.toByteArray();
    }

    private Element element(Document document, String namespace, String name) {
        return document.createElementNS(namespace, name);
    }

    private Element child(Document document, Element parent, String namespace, String name) {
        Element child = element(document, namespace, name);
        parent.appendChild(child);
        return child;
    }

    private void text(Document document, Element parent, String namespace, String name, String value) {
        child(document, parent, namespace, name).setTextContent(value);
    }

    private void require(byte[] value, String message) {
        if (value == null || value.length == 0) throw new IllegalArgumentException(message);
    }

    private record SigningIdentity(PrivateKey privateKey, List<X509Certificate> chain) {}
}
