package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import proyecto.dto.CiudadGetDTO;
import proyecto.dto.LoginCuentaDTO;
import proyecto.dto.LoginDTO;
import proyecto.dto.TokenDTO;
import proyecto.repositorios.CuentaRepo;
import proyecto.servicios.interfaces.AutenticacionServicio;
import proyecto.utils.JWTUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AutenticacionServicioImpl implements AutenticacionServicio {

    private final CuentaRepo cuentaRepo;
    private final JWTUtils jwtUtils;

    @Override
    public TokenDTO login(LoginDTO loginDTO) throws Exception {

        // Buscar credenciales de login sin hidratar relaciones pesadas (ej. ciudad del vendedor)
        LoginCuentaDTO cuenta = cuentaRepo.findLoginByCorreo(loginDTO.email())
                .orElseThrow(() -> new RuntimeException("Credenciales incorrectas"));

        if ("vendedor".equals(cuenta.getRol()) && (cuenta.getEstado() == null || cuenta.getEstado() != 1)) {
            throw new RuntimeException(
                    "El vendedor se encuentra desactivado. Comuníquese con el administrador."
            );
        }

        // Validar contraseña
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        if (!passwordEncoder.matches(loginDTO.password(), cuenta.getPassword())) {
            throw new Exception("La contraseña ingresada es incorrecta");
        }

        // Generar y retornar token
        return new TokenDTO(crearToken(cuenta));
    }


    @Override
    public List<CiudadGetDTO> listarCiudades() {
        return List.of();
    }

    private String crearToken(LoginCuentaDTO cuenta) {
        Map<String, Object> map = new HashMap<>();
        map.put("rol", cuenta.getRol());
        map.put("nombre", cuenta.getNombre());
        map.put("id", cuenta.getCodigo());

        return jwtUtils.generarToken(cuenta.getCorreo(), map);
    }
}
