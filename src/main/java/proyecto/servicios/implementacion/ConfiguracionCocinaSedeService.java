package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import proyecto.dto.ImpresionCocinaSedeDTO;
import proyecto.entidades.Sede;
import proyecto.repositorios.SedeRepository;
import proyecto.repositorios.VendedorRepository;
import proyecto.utils.JWTUtils;

@Service
@RequiredArgsConstructor
public class ConfiguracionCocinaSedeService {
    private final SedeRepository sedeRepository;
    private final VendedorRepository vendedorRepository;
    private final AdministradorAccesoService accesoService;
    private final JWTUtils jwtUtils;

    @Transactional(readOnly = true)
    public ImpresionCocinaSedeDTO obtener(String authorization, Long sedeId) {
        var claims = jwtUtils.parseJwt(authorization.replace("Bearer ", "")).getBody();
        String rol = claims.get("rol", String.class);
        if ("produccion".equals(rol)) {
            var usuario = vendedorRepository.findByCorreoIgnoreCase(claims.getSubject())
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            if (usuario.getSede() == null || !sedeId.equals(usuario.getSede().getId())) {
                throw new IllegalArgumentException("No tiene permisos para acceder a la sede seleccionada");
            }
        } else {
            accesoService.validarAccesoAutenticadoASede(authorization, sedeId);
        }
        Sede sede = buscar(sedeId);
        return new ImpresionCocinaSedeDTO(sede.getId(), Boolean.TRUE.equals(sede.getImpresionCocinaHabilitada()));
    }

    @Transactional
    public ImpresionCocinaSedeDTO actualizar(String authorization, Long sedeId, Boolean habilitada) {
        var admin = accesoService.obtenerAdministradorAutenticado(authorization);
        accesoService.validarAccesoASede(admin, sedeId);
        if (habilitada == null) {
            throw new IllegalArgumentException("El estado de impresion de cocina es obligatorio");
        }
        Sede sede = buscar(sedeId);
        sede.setImpresionCocinaHabilitada(habilitada);
        sedeRepository.save(sede);
        return new ImpresionCocinaSedeDTO(sede.getId(), habilitada);
    }

    private Sede buscar(Long sedeId) {
        return sedeRepository.findById(sedeId)
                .orElseThrow(() -> new IllegalArgumentException("Sede no encontrada"));
    }
}
