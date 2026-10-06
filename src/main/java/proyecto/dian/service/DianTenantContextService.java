package proyecto.dian.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import proyecto.entidades.Administrador;
import proyecto.entidades.Empresa;
import proyecto.servicios.implementacion.AdministradorAccesoService;

@Service
@RequiredArgsConstructor
public class DianTenantContextService {
    private final AdministradorAccesoService access;

    public Empresa requireCompanyAdministrator(String authorization) {
        Administrador administrator = access.obtenerAdministradorAutenticado(authorization);
        if (administrator.getEmpresa() == null || administrator.getEmpresa().getNit() == null) {
            throw new IllegalArgumentException("El administrador no tiene una empresa asociada");
        }
        if (!administrator.isEsAdministradorEmpresa()) {
            throw new IllegalArgumentException("Solo el administrador principal de la empresa puede configurar DIAN");
        }
        return administrator.getEmpresa();
    }
}
