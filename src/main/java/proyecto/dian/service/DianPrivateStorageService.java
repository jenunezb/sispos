package proyecto.dian.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;

@Service
public class DianPrivateStorageService {
    private final Path root;

    public DianPrivateStorageService(@Value("${dian.storage.path:./data/dian-private}") String rootPath) {
        this.root = Path.of(rootPath).toAbsolutePath().normalize();
    }

    public String storeXml(Long companyNit, Long documentId, String stage, byte[] content) {
        if (companyNit == null || companyNit <= 0 || documentId == null || documentId <= 0) {
            throw new IllegalArgumentException("Empresa y documento son obligatorios");
        }
        if (stage == null || !stage.matches("request|signed|response")) {
            throw new IllegalArgumentException("Etapa de almacenamiento no válida");
        }
        if (content == null || content.length == 0) throw new IllegalArgumentException("El XML no puede estar vacío");
        try {
            Path directory = root.resolve(companyNit.toString()).resolve(documentId.toString()).normalize();
            ensureInsideRoot(directory);
            Files.createDirectories(directory);
            Path destination = directory.resolve(stage + ".xml");
            Path temporary = Files.createTempFile(directory, stage + "-", ".tmp");
            try {
                Files.write(temporary, content, StandardOpenOption.TRUNCATE_EXISTING);
                try {
                    Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException ignored) {
                    Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
            return root.relativize(destination).toString().replace('\\', '/');
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible almacenar el documento DIAN", exception);
        }
    }

    private void ensureInsideRoot(Path path) {
        if (!path.startsWith(root)) throw new IllegalArgumentException("Ruta de almacenamiento no válida");
    }
}
