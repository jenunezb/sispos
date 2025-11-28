package proyecto.servicios.interfaces;

import proyecto.dto.CiudadGetDTO;
import proyecto.dto.LoginDTO;
import proyecto.dto.TokenDTO;

import java.util.List;

public interface AutenticacionServicio {
    TokenDTO login(LoginDTO dto) throws Exception;
    List<CiudadGetDTO> listarCiudades();

}