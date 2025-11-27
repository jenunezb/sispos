package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import proyecto.entidades.Vendedor;

import java.util.Optional;

@Repository
public interface VendedorRepository extends JpaRepository<Vendedor, Long> {
    Vendedor findByCorreo(String correo);

    Optional<Vendedor> findByCedula(String cedula);

    boolean existsByCedula (String cedula);

    @Query("SELECT e FROM Vendedor e WHERE e.cedula = :cedula")
    Vendedor findBycedula(@Param("cedula") String cedula);
}
