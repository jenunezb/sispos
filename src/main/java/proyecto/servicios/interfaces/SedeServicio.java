package proyecto.servicios.interfaces;

import proyecto.dto.SedeActualizarDTO;
import proyecto.dto.SedeCrearDTO;
import proyecto.dto.SedeDTO;
import proyecto.entidades.Sede;

import java.util.List;

public interface SedeServicio {

    SedeDTO crear(SedeCrearDTO dto, Long empresaNit);

    List<SedeDTO> listar();

    List<SedeDTO> listar(List<Sede> sedes);

    List<SedeDTO> listarPorEmpresa(Long empresaNit);

    SedeDTO actualizar(SedeActualizarDTO dto);

    SedeDTO obtenerPorId(Long id);

}
