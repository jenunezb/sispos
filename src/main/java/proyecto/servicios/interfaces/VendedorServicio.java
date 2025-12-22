package proyecto.servicios.interfaces;

import proyecto.dto.VendedorDTO;
import proyecto.entidades.Vendedor;

import java.util.List;

public interface VendedorServicio {
    void cambiarEstado(Long codigo, Boolean estado);
    List<VendedorDTO> listarVendedores();
    Vendedor obtenerVendedorPorCorreo(String correo);
}
