package proyecto.servicios.interfaces;

import proyecto.dto.CajaAperturaDTO;
import proyecto.dto.CajaCierreDTO;
import proyecto.dto.CajaTurnoResponseDTO;
import proyecto.entidades.Administrador;

import java.time.LocalDateTime;
import java.util.List;

public interface CajaTurnoServicio {

    CajaTurnoResponseDTO abrirCaja(Administrador administrador, CajaAperturaDTO dto);

    CajaTurnoResponseDTO cerrarCaja(Administrador administrador, Long cajaId, CajaCierreDTO dto);

    CajaTurnoResponseDTO obtenerCajaActual(Long sedeId);

    CajaTurnoResponseDTO obtenerPorId(Long cajaId);

    List<CajaTurnoResponseDTO> listar(Long empresaNit, Long sedeId, LocalDateTime desde, LocalDateTime hasta);
}
