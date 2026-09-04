package proyecto.dian.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import proyecto.dian.model.DianElectronicDocument;
import proyecto.dian.model.DianDocumentType;
import proyecto.dian.model.DianEnvironment;

import java.util.Optional;

public interface DianElectronicDocumentRepository extends JpaRepository<DianElectronicDocument, Long> {
    Optional<DianElectronicDocument> findByEmpresaNitAndEnvironmentAndVentaIdAndDocumentType(
            Long empresaNit, DianEnvironment environment, Long ventaId, DianDocumentType documentType);
}
