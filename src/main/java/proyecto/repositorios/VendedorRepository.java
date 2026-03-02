package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import proyecto.entidades.Vendedor;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendedorRepository extends JpaRepository<Vendedor, Long> {

    Optional<Vendedor> findByCorreo(String correo);

    Optional<Vendedor> findByCedula(String cedula);

    boolean existsByCedula (String cedula);

    @Query("SELECT e FROM Vendedor e WHERE e.cedula = :cedula")
    Vendedor findBycedula(@Param("cedula") String cedula);

    List<Vendedor> findAllByOrderByNombreAsc();

    @Query("""
            SELECT v
            FROM Vendedor v
            WHERE v.empresa.nit = :empresaNit
               OR (v.empresa IS NULL AND v.sede.empresa.nit = :empresaNit)
            ORDER BY v.nombre ASC
            """)
    List<Vendedor> findVisiblesByEmpresaNit(@Param("empresaNit") Long empresaNit);

}
