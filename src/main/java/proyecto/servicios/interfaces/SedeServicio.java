package proyecto.servicios.interfaces;

import proyecto.dto.SedeActualizarDTO;
import proyecto.dto.SedeCrearDTO;
import proyecto.dto.SedeDTO;

import java.util.List;

public interface SedeServicio {

    SedeDTO crear(SedeCrearDTO dto);

    List<SedeDTO> listar();

    List<SedeDTO> listarPorEmpresa(Long empresaNit);

    SedeDTO actualizar(SedeActualizarDTO dto);

    SedeDTO obtenerPorId(Long id);

}
