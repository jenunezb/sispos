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
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class DianXadesSignerTest {
    private static final char[] PASSWORD = "solo-pruebas".toCharArray();

    @Test
    void createsCryptographicallyValidDianXadesEpesSignature() throws Exception {
        TestIdentity identity = identity();
        byte[] unsigned = new DianUblInvoiceBuilder().build(DianUblInvoiceBuilderTest.invoice("Producto firmado"));
        byte[] signed = new DianXadesSigner().sign(unsigned, identity.pkcs12(), new String(PASSWORD),
                OffsetDateTime.of(2026, 9, 4, 20, 15, 0, 0, ZoneOffset.ofHours(-5)));
        Document document = parse(signed);

        assertEquals(2, document.getElementsByTagNameNS(DianUblInvoiceBuilder.EXT_NS, "UBLExtension").getLength());
        assertEquals(DianXadesSigner.POLICY_URL,
                document.getElementsByTagNameNS(DianXadesSigner.XADES_NS, "Identifier").item(0).getTextContent());
        assertEquals(DianXadesSigner.POLICY_SHA256,
                document.getElementsByTagNameNS(DianXadesSigner.XADES_NS, "SigPolicyHash").item(0)
                        .getFirstChild().getNextSibling().getTextContent());
        assertEquals(1, document.getElementsByTagNameNS(DianXadesSigner.XADES_NS, "SigningCertificate").getLength());

        Element signatureElement = (Element) document.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature").item(0);
        Element signedProperties = (Element) document.getElementsByTagNameNS(
                DianXadesSigner.XADES_NS, "SignedProperties").item(0);
        Element keyInfo = (Element) document.getElementsByTagNameNS(XMLSignature.XMLNS, "KeyInfo").item(0);
        DOMValidateContext context = new DOMValidateContext(identity.certificate().getPublicKey(), signatureElement);
        context.setIdAttributeNS(signedProperties, null, "Id");
        context.setIdAttributeNS(keyInfo, null, "Id");
        XMLSignature signature = XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(context);
        assertTrue(signature.validate(context));
        assertEquals(3, signature.getSignedInfo().getReferences().size());
    }

    @Test
    void rejectsAFileWithoutPrivateKey() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new DianXadesSigner().sign("<Invoice/>".getBytes(), new byte[]{1, 2, 3}, "incorrecta"));
        assertNotNull(error.getMessage());
    }

    private TestIdentity identity() throws Exception {
        if (Security.getProvider("BC") == null) Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, new SecureRandom());
        var pair = generator.generateKeyPair();
        X500Name name = new X500Name("CN=Steelsoft Test,O=Steelsoft,C=CO");
        Date from = Date.from(java.time.Instant.parse("2026-01-01T00:00:00Z"));
        Date until = Date.from(java.time.Instant.parse("2027-01-01T00:00:00Z"));
        var builder = new JcaX509v3CertificateBuilder(name, BigInteger.ONE, from, until, name, pair.getPublic());
        var signer = new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(pair.getPrivate());
        X509Certificate certificate = new JcaX509CertificateConverter().setProvider("BC")
                .getCertificate(builder.build(signer));
        KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(null, PASSWORD);
        store.setKeyEntry("steelsoft-test", pair.getPrivate(), PASSWORD,
                new java.security.cert.Certificate[]{certificate});
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        store.store(output, PASSWORD);
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
