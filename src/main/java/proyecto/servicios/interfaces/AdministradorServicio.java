package proyecto.servicios.interfaces;

import proyecto.dto.AdministradorDTO;
import proyecto.dto.UsuarioDTO;

public interface AdministradorServicio {

    int crearVendedor (UsuarioDTO usuarioDTO) throws Exception;

    int crearAdministrador(AdministradorDTO administradorDTO) throws Exception;

}
