package proyecto.repositorios;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import proyecto.entidades.TipoSalida;

@Repository
public interface TipoSalidaRepository extends JpaRepository<TipoSalida, Long> {
}
