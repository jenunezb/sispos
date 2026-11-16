package proyecto.dian.service;

import org.junit.jupiter.api.Test;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class DianSoapMessageServiceTest {
    private final DianSoapMessageService service = new DianSoapMessageService();

    @Test
    void buildsSendTestSetAsyncWithoutLosingZipBytes() throws Exception {
        byte[] zip = new byte[]{80, 75, 3, 4, 10, 20};
        byte[] soap = service.sendTestSetAsync("fv090209186400001.zip", zip, "set-id-123");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        var document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(soap));

        assertEquals("fv090209186400001.zip", document.getElementsByTagNameNS("*", "fileName").item(0).getTextContent());
        assertArrayEquals(zip, Base64.getDecoder().decode(
                document.getElementsByTagNameNS("*", "contentFile").item(0).getTextContent()));
        assertEquals("set-id-123", document.getElementsByTagNameNS("*", "testSetId").item(0).getTextContent());
    }

    @Test
    void parsesTrackIdAndStatusFromDianResponse() {
        String response = """
                <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope">
                  <s:Body><SendTestSetAsyncResponse xmlns="http://wcf.dian.colombia">
                    <SendTestSetAsyncResult><ZipKey>track-123</ZipKey><IsValid>true</IsValid>
                    <StatusCode>00</StatusCode><StatusDescription>Procesado correctamente</StatusDescription>
                    </SendTestSetAsyncResult>
                  </SendTestSetAsyncResponse></s:Body>
                </s:Envelope>
                """;
        var result = service.parseResponse(response.getBytes(StandardCharsets.UTF_8));
        assertEquals("track-123", result.zipKey());
        assertTrue(result.valid());
        assertEquals("00", result.statusCode());
    }

    @Test
    void buildsGetStatusZipWithTrackId() throws Exception {
        byte[] soap = service.getStatusZip("track-123");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        var document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(soap));
        assertEquals(1, document.getElementsByTagNameNS(DianSoapMessageService.DIAN_NS, "GetStatusZip").getLength());
        assertEquals("track-123", document.getElementsByTagNameNS("*", "trackId").item(0).getTextContent());
    }

    @Test
    void convertsSoapFaultIntoSanitizedResult() {
        String response = """
                <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope"><s:Body><s:Fault>
                <s:Reason><s:Text xml:lang="es">Solicitud rechazada</s:Text></s:Reason>
                </s:Fault></s:Body></s:Envelope>
                """;
        var result = service.parseResponse(response.getBytes(StandardCharsets.UTF_8));
        assertFalse(result.valid());
        assertEquals("Solicitud rechazada", result.statusDescription());
    }

    @Test
    void rejectsDoctypeToPreventExternalEntityReads() {
        byte[] malicious = "<!DOCTYPE x [<!ENTITY e SYSTEM 'file:///etc/passwd'>]><x>&e;</x>"
                .getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> service.parseResponse(malicious));
    }
}
