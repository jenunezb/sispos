package proyecto.dian.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class DianPrivateStorageServiceTest {
    @TempDir Path directory;

    @Test
    void storesTenantDocumentUsingOnlyRelativeReference() throws Exception {
        var service = new DianPrivateStorageService(directory.toString());
        String reference = service.storeXml(902091864L, 10L, "request",
                "<Invoice/>".getBytes(StandardCharsets.UTF_8));
        assertEquals("902091864/10/request.xml", reference);
        assertEquals("<Invoice/>", Files.readString(directory.resolve(reference)));
        assertFalse(reference.contains(directory.toString()));
    }

    @Test
    void rejectsUncontrolledStageNames() {
        var service = new DianPrivateStorageService(directory.toString());
        assertThrows(IllegalArgumentException.class,
                () -> service.storeXml(1L, 1L, "../../public", new byte[]{1}));
    }
}
