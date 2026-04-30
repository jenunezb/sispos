package proyecto.servicios.interfaces;

import proyecto.dto.ComandaCocinaCrearDTO;
import proyecto.dto.ComandaCocinaResponseDTO;
import proyecto.entidades.EstadoComandaCocina;

import java.util.List;

public interface ComandaCocinaServicio {

    ComandaCocinaResponseDTO crearComanda(ComandaCocinaCrearDTO dto);

    List<ComandaCocinaResponseDTO> listarComandasActivas(String correo);

    ComandaCocinaResponseDTO actualizarEstado(String correo, Long comandaId, EstadoComandaCocina estado);
}
