package proyecto.dian.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import proyecto.dian.dto.DianCompanyFiscalProfile;
import proyecto.entidades.Empresa;
import proyecto.repositorios.EmpresaRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DianCompanyFiscalProfileServiceTest {
    private final DianTenantContextService tenant = mock(DianTenantContextService.class);
    private final EmpresaRepository companies = mock(EmpresaRepository.class);
    private final DianCompanyFiscalProfileService service = new DianCompanyFiscalProfileService(tenant, companies);
    private Empresa company;

    @BeforeEach
    void setUp() {
        company = new Empresa();
        company.setNit(1097726190L);
        when(tenant.requireCompanyAdministrator("Bearer company-admin")).thenReturn(company);
        when(companies.save(any(Empresa.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void storesOptionalFiscalProfileForAuthenticatedCompany() {
        var request = new DianCompanyFiscalProfile("31", "2", " Delicias de hojaldre ", null,
                "R-99-PN", "49", "ZZ", "No aplica", "Calle 20 # 15-25", "63001", "Armenia",
                "63", "Quindío", "co", "Colombia", "630001", " FACTURAS@EJEMPLO.COM ", "3150186263");

        DianCompanyFiscalProfile response = service.update("Bearer company-admin", request);

        assertEquals("Delicias de hojaldre", response.razonSocial());
        assertEquals("CO", response.paisCodigo());
        assertEquals("facturas@ejemplo.com", response.correoFacturacion());
        verify(companies).save(company);
    }

    @Test
    void acceptsEmptyFieldsWithoutChangingNormalCompanyOperation() {
        var empty = new DianCompanyFiscalProfile("", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "");

        DianCompanyFiscalProfile response = service.update("Bearer company-admin", empty);

        assertNull(response.tipoDocumentoFiscal());
        assertNull(response.direccionFiscal());
        assertNull(response.correoFacturacion());
        verify(companies).save(company);
    }
}
