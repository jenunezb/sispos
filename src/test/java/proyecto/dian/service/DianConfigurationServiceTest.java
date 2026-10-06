package proyecto.dian.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import proyecto.dian.dto.DianConfigurationUpdateRequest;
import proyecto.dian.model.*;
import proyecto.dian.repository.DianConfigurationRepository;
import proyecto.dian.repository.DianTransmissionAttemptRepository;
import proyecto.entidades.Empresa;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DianConfigurationServiceTest {
    private final DianConfigurationRepository configurations = mock(DianConfigurationRepository.class);
    private final DianTransmissionAttemptRepository audit = mock(DianTransmissionAttemptRepository.class);
    private final DianTenantContextService tenant = mock(DianTenantContextService.class);
    private DianConfigurationService service;
    private Empresa company;

    @BeforeEach
    void setUp() {
        byte[] key = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        service = new DianConfigurationService(configurations, audit, tenant,
                new DianCryptoService(Base64.getEncoder().encodeToString(key)));
        company = new Empresa();
        company.setNit(900123456L);
        when(tenant.requireCompanyAdministrator("Bearer tenant-a")).thenReturn(company);
        when(configurations.save(any())).thenAnswer(invocation -> {
            DianConfiguration value = invocation.getArgument(0);
            value.setId(7L);
            return value;
        });
    }

    @Test
    void storesSecretsEncryptedAndReturnsOnlyIndicators() {
        when(configurations.findByEmpresaNitAndEnvironment(900123456L, DianEnvironment.HABILITACION))
                .thenReturn(Optional.empty());
        var request = new DianConfigurationUpdateRequest(DianEnvironment.HABILITACION,
                DianOperationMode.SOFTWARE_PROPIO, "software-1", "pin-visible-only-on-input",
                "test-set", "technical-key", "SETT", 1L, 100L, "RES-TEST", null, null);

        var response = service.update("Bearer tenant-a", request);

        assertTrue(response.softwarePinConfigured());
        assertTrue(response.technicalKeyConfigured());
        assertEquals(DianConfigurationStatus.CONFIGURED, response.status());
        verify(configurations).save(argThat(saved ->
                saved.getEmpresa() == company
                        && !saved.getSoftwarePinEncrypted().contains("pin-visible-only-on-input")
                        && !saved.getTechnicalKeyEncrypted().contains("technical-key")));
        verify(audit).save(argThat(item -> item.getEmpresa() == company
                && "CONFIGURATION_UPDATE".equals(item.getOperation())));
    }

    @Test
    void alwaysScopesLookupToAuthenticatedCompany() {
        service.get("Bearer tenant-a", DianEnvironment.PRODUCCION);

        verify(configurations).findByEmpresaNitAndEnvironment(900123456L, DianEnvironment.PRODUCCION);
        verify(configurations, never()).findById(anyLong());
    }

    @Test
    void rejectsInvalidRangeBeforeSaving() {
        when(configurations.findByEmpresaNitAndEnvironment(900123456L, DianEnvironment.HABILITACION))
                .thenReturn(Optional.empty());
        var request = new DianConfigurationUpdateRequest(DianEnvironment.HABILITACION,
                DianOperationMode.SOFTWARE_PROPIO, "software", null, null, null,
                "SETT", 50L, 10L, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> service.update("Bearer tenant-a", request));
        verify(configurations, never()).save(any());
    }
}
