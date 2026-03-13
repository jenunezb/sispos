package proyecto.servicios.interfaces;

import org.springframework.web.multipart.MultipartFile;
import proyecto.dto.*;

import java.time.LocalDate;
import java.util.List;

public interface AdministradorServicio {

    int crearVendedor (UsuarioDTO usuarioDTO, Long empresaNit) throws Exception;

    int registrarEmpresa(RegistroEmpresaDTO dto, MultipartFile archivo) throws Exception;

    void editarVendedor(UsuarioDTO usuarioDTO);

    List<InventarioFinalDTO> obtenerInventarioFinal(
            Long sedeId,
            LocalDate fechaInicio,
            LocalDate fechaFin
    );
        void cambiarPassword(String correo, String passwordActual, String passwordNueva) throws Exception;

}
