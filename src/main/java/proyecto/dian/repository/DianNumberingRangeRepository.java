package proyecto.dian.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import proyecto.dian.model.DianNumberingRange;

import java.util.List;

public interface DianNumberingRangeRepository extends JpaRepository<DianNumberingRange, Long> {
    List<DianNumberingRange> findByEmpresaNitOrderByCreatedAtDesc(Long empresaNit);
}
