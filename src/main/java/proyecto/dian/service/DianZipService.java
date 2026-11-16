package proyecto.dian.service;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class DianZipService {
    public byte[] zipXml(String xmlFileName, byte[] xml) {
        if (xmlFileName == null || !xmlFileName.matches("[A-Za-z0-9_-]+\\.xml")) {
            throw new IllegalArgumentException("El nombre del XML no es válido");
        }
        if (xml == null || xml.length == 0) {
            throw new IllegalArgumentException("El XML no puede estar vacío");
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(output)) {
                zip.putNextEntry(new ZipEntry(xmlFileName));
                zip.write(xml);
                zip.closeEntry();
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible comprimir el XML DIAN", exception);
        }
    }
}
