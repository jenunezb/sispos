package proyecto.servicios.interfaces;

import proyecto.dto.VendedorDTO;

import java.util.List;

public interface VendedorServicio {
    void cambiarEstado(Long codigo, Boolean estado);
    List<VendedorDTO> listarVendedores();
}
