package proyecto.repositorios;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import proyecto.entidades.Ciudad;

@Repository
public interface CiudadRepo  extends JpaRepository<Ciudad, Integer> {

    Ciudad findByNombre(String ciudad);

    @Transactional
    @Modifying
    @Query("DELETE FROM Ciudad c WHERE c.nombre = :nombreCiudad")
    void deleteByNombre(String nombreCiudad);
}