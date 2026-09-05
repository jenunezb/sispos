package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import proyecto.entidades.Empresa;

import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    // Verificar si ya existe una empresa con ese NIT
    boolean existsByNit(Long nit);

    // Buscar empresa por nombre
    Optional<Empresa> findByNombre(String nombre);

}
