package proyecto.servicios.interfaces;

import proyecto.dto.BalanceSedeVendedor;
import proyecto.dto.VendedorDTO;
import proyecto.entidades.Vendedor;

import java.time.LocalDateTime;
import java.util.List;

public interface VendedorServicio {
    void cambiarEstado(Long codigo, Boolean estado);
    void eliminarVendedor(Long codigo, Long empresaNit, List<Long> sedeIdsVisibles);
    List<VendedorDTO> listarVendedores(Long empresaNit);
    List<VendedorDTO> listarVendedores(Long empresaNit, List<Long> sedeIds);
    Vendedor obtenerVendedorPorCorreo(String correo);
    BalanceSedeVendedor balancePorSedeId(String email, LocalDateTime desde, LocalDateTime hasta);
}
