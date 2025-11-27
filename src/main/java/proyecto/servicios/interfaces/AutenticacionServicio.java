package proyecto.servicios.interfaces;

import proyecto.dto.LoginDTO;
import proyecto.dto.TokenDTO;

public interface AutenticacionServicio {
    TokenDTO login(LoginDTO dto) throws Exception;
}
