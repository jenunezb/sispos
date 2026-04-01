package proyecto.servicios.interfaces;

import proyecto.dto.MesaEstadoDTO;

import java.util.List;

public interface MesaEstadoServicio {

    List<MesaEstadoDTO> listarPorSede(String correo, String rol, Long sedeId);

    MesaEstadoDTO guardarMesa(String correo, String rol, Long sedeId, Long mesaId, MesaEstadoDTO dto);
}
