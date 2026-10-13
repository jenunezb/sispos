package proyecto.dian.service;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

class DianZipServiceTest {
    private final DianZipService service = new DianZipService();

    @Test
    void createsSingleEntryZipWithOriginalXml() throws Exception {
        byte[] xml = "<Invoice/>".getBytes(StandardCharsets.UTF_8);
        byte[] zipped = service.zipXml("fv09020918640000000001.xml", xml);

        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(zipped))) {
            assertEquals("fv09020918640000000001.xml", input.getNextEntry().getName());
            assertArrayEquals(xml, input.readAllBytes());
            assertNull(input.getNextEntry());
        }
    }

    @Test
    void preventsPathTraversalAndEmptyDocuments() {
        assertThrows(IllegalArgumentException.class, () -> service.zipXml("../factura.xml", new byte[]{1}));
        assertThrows(IllegalArgumentException.class, () -> service.zipXml("factura.xml", new byte[0]));
    }
}
