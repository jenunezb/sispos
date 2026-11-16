package proyecto.servicios.interfaces;

import proyecto.dto.SuperAdminConfigurarSuscripcionDTO;
import proyecto.dto.SuperAdminPagoSuscripcionDTO;
import proyecto.dto.SuperAdminRegistrarPagoSuscripcionDTO;
import proyecto.dto.SuperAdminSuscripcionSedeDTO;

import java.util.List;

public interface SuperAdminSuscripcionServicio {

    SuperAdminSuscripcionSedeDTO configurarSuscripcion(SuperAdminConfigurarSuscripcionDTO dto);

    SuperAdminPagoSuscripcionDTO registrarPago(SuperAdminRegistrarPagoSuscripcionDTO dto, String correoRegistrador);

    List<SuperAdminSuscripcionSedeDTO> listarSuscripciones();

    SuperAdminSuscripcionSedeDTO obtenerSuscripcionPorSede(Long sedeId);

    List<SuperAdminPagoSuscripcionDTO> listarPagos(Long sedeId);
}
