package proyecto.servicios.implementacion;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import proyecto.entidades.Administrador;
import proyecto.entidades.Sede;
import proyecto.repositorios.AdministradorRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.utils.JWTUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdministradorAccesoService {

    private final JWTUtils jwtUtils;
    private final AdministradorRepository administradorRepository;
    private final SedeRepository sedeRepository;

    public Administrador obtenerAdministradorAutenticado(String authorization) {
        String token = authorization.replace("Bearer ", "");
        Jws<Claims> claims = jwtUtils.parseJwt(token);
        String correo = claims.getBody().getSubject();
        String rol = (String) claims.getBody().get("rol");

        if (!"administrador".equals(rol)) {
            throw new RuntimeException("No tiene permisos para acceder a este recurso");
        }

        return administradorRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Administrador no encontrado"));
    }

    public void validarAdministradorEmpresa(Administrador administrador) {
        if (administrador.isEsSuperAdmin()) {
            return;
        }

        if (!administrador.isEsAdministradorEmpresa()) {
            throw new RuntimeException("Solo el administrador principal de la empresa puede realizar esta accion");
        }
    }

    public boolean tieneAccesoASede(Administrador administrador, Long sedeId) {
        if (administrador.isEsSuperAdmin()) {
            return true;
        }

        if (administrador.getEmpresa() == null) {
            return false;
        }

        Sede sede = sedeRepository.findById(sedeId)
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        if (sede.getEmpresa() == null || !administrador.getEmpresa().getNit().equals(sede.getEmpresa().getNit())) {
            return false;
        }

        if (administrador.isEsAdministradorEmpresa()) {
            return true;
        }

        return administradorRepository.findDetalleByCodigo(administrador.getCodigo())
                .orElseThrow(() -> new RuntimeException("Administrador no encontrado"))
                .getSedesAsignadas()
                .stream()
                .anyMatch(sedeAsignada -> sedeAsignada.getId().equals(sedeId));
    }

    public void validarAccesoASede(Administrador administrador, Long sedeId) {
        if (!tieneAccesoASede(administrador, sedeId)) {
            throw new RuntimeException("No tiene permisos para acceder a la sede seleccionada");
        }
    }

    public List<Sede> obtenerSedesVisibles(Administrador administrador) {
        if (administrador.isEsSuperAdmin()) {
            return sedeRepository.findAll();
        }

        if (administrador.getEmpresa() == null) {
            throw new RuntimeException("El administrador no tiene una empresa asociada");
        }

        if (administrador.isEsAdministradorEmpresa()) {
            return sedeRepository.findByEmpresaNit(administrador.getEmpresa().getNit());
        }

        return sedeRepository.findByAdministradorAsignado(administrador.getCodigo());
    }

    public Long resolverEmpresaNit(Administrador administrador, Long empresaNitSolicitada) {
        if (administrador.isEsSuperAdmin()) {
            if (empresaNitSolicitada == null) {
                throw new RuntimeException("Debe indicar el parametro empresaNit para esta operacion");
            }
            return empresaNitSolicitada;
        }

        if (administrador.getEmpresa() == null) {
            throw new RuntimeException("El administrador no tiene una empresa asociada");
        }

        return administrador.getEmpresa().getNit();
    }
}
