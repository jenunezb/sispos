package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import proyecto.entidades.Imagen;

import java.util.Optional;

@Repository
public interface ImagenRepository extends JpaRepository<Imagen, Long> {

    Optional<Imagen> findByPublicId(String publicId);

}
