package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import proyecto.dto.LoginCuentaDTO;
import proyecto.entidades.Cuenta;

import java.util.Optional;

@Repository
public interface CuentaRepo extends JpaRepository<Cuenta, Integer> {

    @Query("select c from Cuenta c where c.correo=:correo")
    Optional<Cuenta> findByCorreo(@Param("correo")String correo);


    @Query(value = """
            SELECT c.codigo AS codigo,
                   c.correo AS correo,
                   c.password AS password,
                   CASE WHEN v.codigo IS NOT NULL THEN 'vendedor' ELSE 'administrador' END AS rol,
                   COALESCE(v.nombre, a.nombre, 'Administrador') AS nombre,
                   CASE WHEN COALESCE(v.estado, true) THEN 1 ELSE 0 END AS estado,
                   COALESCE(ea.nombre, ev.nombre, evs.nombre) AS nombreEmpresa
            FROM cuenta c
            LEFT JOIN vendedor v ON v.codigo = c.codigo
            LEFT JOIN administrador a ON a.codigo = c.codigo
            LEFT JOIN empresa ea ON ea.nit = a.empresa_id
            LEFT JOIN empresa ev ON ev.nit = v.empresa_id
            LEFT JOIN sede sv ON sv.id = v.sede_id
            LEFT JOIN empresa evs ON evs.nit = sv.empresa_id
            WHERE c.correo = :correo
            LIMIT 1
            """, nativeQuery = true)
    Optional<LoginCuentaDTO> findLoginByCorreo(@Param("correo") String correo);

}
