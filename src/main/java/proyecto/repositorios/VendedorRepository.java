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

    Optional<Vendedor> findByCorreoIgnoreCase(String correo);

    Optional<Vendedor> findByCedula(String cedula);

    boolean existsByCedula(String cedula);

    @Query("SELECT e FROM Vendedor e WHERE e.cedula = :cedula")
    Vendedor findBycedula(@Param("cedula") String cedula);

    List<Vendedor> findAllByOrderByNombreAsc();

    @Query("""
            SELECT v
            FROM Vendedor v
            LEFT JOIN v.empresa empresa
            LEFT JOIN v.sede sede
            LEFT JOIN sede.empresa empresaSede
            WHERE empresa.nit = :empresaNit
               OR (empresa IS NULL AND empresaSede.nit = :empresaNit)
            ORDER BY v.nombre ASC
            """)
    List<Vendedor> findVisiblesByEmpresaNit(@Param("empresaNit") Long empresaNit);

    @Query("""
            SELECT v
            FROM Vendedor v
            LEFT JOIN v.empresa empresa
            LEFT JOIN v.sede sede
            LEFT JOIN sede.empresa empresaSede
            WHERE (empresa.nit = :empresaNit
               OR (empresa IS NULL AND empresaSede.nit = :empresaNit))
              AND sede.id IN :sedeIds
            ORDER BY v.nombre ASC
            """)
    List<Vendedor> findVisiblesByEmpresaNitAndSedeIdIn(
            @Param("empresaNit") Long empresaNit,
            @Param("sedeIds") List<Long> sedeIds
    );

}
