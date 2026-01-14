package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import proyecto.entidades.InformeInventarioDia;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InformeInventarioDiaRepository extends JpaRepository<InformeInventarioDia, Long> {
    List<InformeInventarioDia> findBySedeIdAndFecha(Long sedeId, LocalDate fecha);
}
