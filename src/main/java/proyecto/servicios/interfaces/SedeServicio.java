package proyecto.servicios.interfaces;

import proyecto.dto.SedeActualizarDTO;
import proyecto.dto.SedeCrearDTO;
import proyecto.dto.SedeDTO;
import proyecto.entidades.Administrador;

import java.util.List;

public interface SedeServicio {

    SedeDTO crear(SedeCrearDTO dto);

    List<SedeDTO> listar();

    SedeDTO actualizar(SedeActualizarDTO dto);

    SedeDTO obtenerPorId(Long id);

}
