package proyecto.servicios.interfaces;

import proyecto.dto.AdministradorDTO;
import proyecto.dto.InventarioFinalDTO;
import proyecto.dto.InventarioFinalProjection;
import proyecto.dto.UsuarioDTO;

import java.time.LocalDate;
import java.util.List;

public interface AdministradorServicio {

    int crearVendedor (UsuarioDTO usuarioDTO) throws Exception;

    int crearAdministrador(AdministradorDTO administradorDTO) throws Exception;

    void editarVendedor(UsuarioDTO usuarioDTO);

    List<InventarioFinalDTO> obtenerInventarioFinal(
            Long sedeId,
            LocalDate fechaInicio,
            LocalDate fechaFin
    );
}
