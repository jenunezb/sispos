package proyecto.dian.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import proyecto.dian.model.DianNumberingRange;
import proyecto.dian.model.DianDocumentType;
import proyecto.dian.model.DianEnvironment;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface DianNumberingRangeRepository extends JpaRepository<DianNumberingRange, Long> {
    List<DianNumberingRange> findByEmpresaNitOrderByCreatedAtDesc(Long empresaNit);

    boolean existsByEmpresaNitAndEnvironmentAndDocumentTypeAndPrefixAndActiveTrue(
            Long empresaNit, DianEnvironment environment, DianDocumentType documentType, String prefix);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select r from DianNumberingRange r
        where r.id = :id and r.empresa.nit = :empresaNit
    """)
    Optional<DianNumberingRange> findByIdForUpdate(@Param("id") Long id, @Param("empresaNit") Long empresaNit);
}
