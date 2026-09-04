package proyecto.dian.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import proyecto.dian.model.DianElectronicDocument;

public interface DianElectronicDocumentRepository extends JpaRepository<DianElectronicDocument, Long> {
}
