package proyecto.dian.service;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class DianWsSecuritySignerTest {
    private static final String PASSWORD = "solo-pruebas";
    private static final String URL = "https://vpfe-hab.dian.gov.co/WcfDianCustomerServices.svc";
    private static final String ACTION = "http://wcf.dian.colombia/IWcfDianCustomerServices/SendTestSetAsync";

    @Test
    void createsValidWsSecuritySignatureWithCertificateAndTimestamp() throws Exception {
        TestIdentity identity = identity();
        byte[] envelope = new DianSoapMessageService().sendTestSetAsync("fv090209186400001.zip",
                new byte[]{80, 75, 3, 4}, "test-set-id");
        byte[] signed = new DianWsSecuritySigner().sign(envelope, identity.pkcs12(), PASSWORD,
                URL, ACTION, Instant.parse("2026-10-13T14:15:16.123Z"));
        Document document = parse(signed);

        Element to = (Element) document.getElementsByTagNameNS(DianWsSecuritySigner.ADDRESSING_NS, "To").item(0);
        Element timestamp = (Element) document.getElementsByTagNameNS(DianWsSecuritySigner.WSU_NS, "Timestamp").item(0);
        Element token = (Element) document.getElementsByTagNameNS(DianWsSecuritySigner.WSSE_NS,
                "BinarySecurityToken").item(0);
        assertEquals(URL, to.getTextContent());
        assertEquals(ACTION, document.getElementsByTagNameNS(DianWsSecuritySigner.ADDRESSING_NS,
                "Action").item(0).getTextContent());
        assertEquals("2026-10-13T14:15:16.123Z", document.getElementsByTagNameNS(
                DianWsSecuritySigner.WSU_NS, "Created").item(0).getTextContent());
        assertEquals("2026-10-13T14:16:16.123Z", document.getElementsByTagNameNS(
                DianWsSecuritySigner.WSU_NS, "Expires").item(0).getTextContent());
        assertArrayEquals(identity.certificate().getEncoded(), Base64.getDecoder().decode(token.getTextContent()));

        Element signatureElement = (Element) document.getElementsByTagNameNS(XMLSignature.XMLNS,
                "Signature").item(0);
        DOMValidateContext context = new DOMValidateContext(identity.certificate().getPublicKey(), signatureElement);
        context.setIdAttributeNS(to, DianWsSecuritySigner.WSU_NS, "Id");
        context.setIdAttributeNS(timestamp, DianWsSecuritySigner.WSU_NS, "Id");
        XMLSignature signature = XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(context);
        assertTrue(signature.validate(context));
        assertEquals(2, signature.getSignedInfo().getReferences().size());
    }

    @Test
    void rejectsExternalEntities() throws Exception {
        TestIdentity identity = identity();
        byte[] malicious = "<!DOCTYPE x [<!ENTITY e SYSTEM 'file:///etc/passwd'>]><x>&e;</x>"
                .getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalStateException.class, () -> new DianWsSecuritySigner().sign(malicious,
                identity.pkcs12(), PASSWORD, URL, ACTION));
    }

    private TestIdentity identity() throws Exception {
        if (Security.getProvider("BC") == null) Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, new SecureRandom());
        var pair = generator.generateKeyPair();
        X500Name name = new X500Name("CN=Steelsoft WS Test,O=Steelsoft,C=CO");
        Date from = Date.from(Instant.parse("2026-01-01T00:00:00Z"));
        Date until = Date.from(Instant.parse("2027-01-01T00:00:00Z"));
        var builder = new JcaX509v3CertificateBuilder(name, BigInteger.TWO, from, until, name, pair.getPublic());
        var signer = new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(pair.getPrivate());
        X509Certificate certificate = new JcaX509CertificateConverter().setProvider("BC")
                .getCertificate(builder.build(signer));
        KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(null, PASSWORD.toCharArray());
        store.setKeyEntry("steelsoft-ws-test", pair.getPrivate(), PASSWORD.toCharArray(),
                new java.security.cert.Certificate[]{certificate});
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        store.store(output, PASSWORD.toCharArray());
        return new TestIdentity(output.toByteArray(), certificate);
    }

    private Document parse(byte[] xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
    }

    private record TestIdentity(byte[] pkcs12, X509Certificate certificate) {}
}
