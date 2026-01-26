package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import proyecto.entidades.TokenValidacion;

import java.util.Optional;

public interface TokenValidacionRepository extends JpaRepository<TokenValidacion, Long> {
    Optional<TokenValidacion> findByToken(String token);
}

