package proyecto.eventos;

import proyecto.dto.MesaEstadoDTO;

public record MesaEstadoActualizadoEvento(Long sedeId, MesaEstadoDTO mesa) {}
