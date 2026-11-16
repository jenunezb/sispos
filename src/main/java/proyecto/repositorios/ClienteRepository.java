package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import proyecto.entidades.Cliente;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    List<Cliente> findByEmpresaNitAndActivoTrueOrderByNombreAsc(Long empresaNit);

    Optional<Cliente> findByIdAndEmpresaNit(Long id, Long empresaNit);
}
