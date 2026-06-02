package proyecto.servicios.interfaces;

import proyecto.dto.GastoDiarioCrearDTO;
import proyecto.dto.GastoDiarioResponseDTO;
import proyecto.entidades.Administrador;

import java.time.LocalDateTime;
import java.util.List;

public interface GastoDiarioServicio {

    GastoDiarioResponseDTO crear(Administrador administrador, GastoDiarioCrearDTO dto);

    List<GastoDiarioResponseDTO> listar(Long empresaNit, Long sedeId, LocalDateTime desde, LocalDateTime hasta);
}
