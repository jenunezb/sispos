package proyecto.dian.service;

import org.junit.jupiter.api.Test;
import proyecto.entidades.Administrador;
import proyecto.entidades.Empresa;
import proyecto.servicios.implementacion.AdministradorAccesoService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DianTenantContextServiceTest {
    private final AdministradorAccesoService access = mock(AdministradorAccesoService.class);
    private final DianTenantContextService service = new DianTenantContextService(access);

    @Test
    void derivesCompanyFromAuthenticatedPrincipal() {
        Empresa company = new Empresa();
        company.setNit(123L);
        Administrador admin = new Administrador();
        admin.setEmpresa(company);
        admin.setEsAdministradorEmpresa(true);
        when(access.obtenerAdministradorAutenticado("Bearer token")).thenReturn(admin);

        assertSame(company, service.requireCompanyAdministrator("Bearer token"));
    }

    @Test
    void rejectsLimitedAdministrator() {
        Empresa company = new Empresa();
        company.setNit(123L);
        Administrador admin = new Administrador();
        admin.setEmpresa(company);
        admin.setEsAdministradorEmpresa(false);
        when(access.obtenerAdministradorAutenticado("Bearer token")).thenReturn(admin);

        assertThrows(IllegalArgumentException.class,
                () -> service.requireCompanyAdministrator("Bearer token"));
    }
}
