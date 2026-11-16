package proyecto.dian.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import proyecto.dian.model.DianNumberingRange;
import proyecto.dian.model.DianConfiguration;
import proyecto.dian.model.DianDocumentType;
import proyecto.dian.model.DianEnvironment;
import proyecto.dian.dto.DianNumberingRangeRequest;
import proyecto.dian.repository.DianConfigurationRepository;
import proyecto.dian.repository.DianNumberingRangeRepository;
import proyecto.entidades.Empresa;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DianNumberingServiceTest {
    @Mock DianNumberingRangeRepository ranges;
    @Mock DianConfigurationRepository configurations;
    @Mock DianTenantContextService tenantContext;
    @Mock DianCryptoService crypto;
    DianNumberingService service;

    @BeforeEach
    void setUp() {
        service = new DianNumberingService(ranges, configurations, tenantContext, crypto);
    }

    @Test
    void allocatesNextNumberUsingTenantBoundDatabaseLock() {
        DianNumberingRange range = range(7L, 100L, 105L, 100L);
        when(ranges.findByIdForUpdate(7L, 902091864L)).thenReturn(Optional.of(range));

        DianNumberingService.AllocatedNumber allocated =
                service.allocate(902091864L, 7L, LocalDate.parse("2026-09-04"));

        assertEquals(101L, allocated.consecutive());
        assertEquals("SETP101", allocated.fullNumber());
        assertEquals(101L, range.getCurrentNumber());
        verify(ranges).findByIdForUpdate(7L, 902091864L);
        verify(ranges).save(range);
    }

    @Test
    void createsRangeBeforeItsFirstNumberSoTheInitialConsecutiveIsNotSkipped() {
        Empresa company = new Empresa();
        company.setNit(902091864L);
        DianConfiguration configuration = new DianConfiguration();
        configuration.setEmpresa(company);
        configuration.setEnvironment(DianEnvironment.HABILITACION);
        var request = new DianNumberingRangeRequest(DianEnvironment.HABILITACION,
                DianDocumentType.INVOICE, "SETP", "18760000001", 990000000L, 995000000L,
                LocalDate.parse("2019-01-19"), LocalDate.parse("2030-01-19"), null);
        when(tenantContext.requireCompanyAdministrator("Bearer token")).thenReturn(company);
        when(configurations.findByEmpresaNitAndEnvironment(company.getNit(), DianEnvironment.HABILITACION))
                .thenReturn(Optional.of(configuration));
        when(ranges.save(any(DianNumberingRange.class))).thenAnswer(call -> call.getArgument(0));

        service.create("Bearer token", request);

        var saved = org.mockito.ArgumentCaptor.forClass(DianNumberingRange.class);
        verify(ranges).save(saved.capture());
        assertEquals(989999999L, saved.getValue().getCurrentNumber());
    }

    @Test
    void rejectsRangeFromAnotherCompanyWithoutChangingIt() {
        when(ranges.findByIdForUpdate(7L, 999L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> service.allocate(999L, 7L, LocalDate.parse("2026-09-04")));
        verify(ranges, never()).save(any());
    }

    @Test
    void rejectsExpiredOrExhaustedRange() {
        DianNumberingRange exhausted = range(7L, 100L, 100L, 100L);
        when(ranges.findByIdForUpdate(7L, 902091864L)).thenReturn(Optional.of(exhausted));
        assertThrows(IllegalStateException.class,
                () -> service.allocate(902091864L, 7L, LocalDate.parse("2026-09-04")));

        DianNumberingRange expired = range(8L, 1L, 20L, 0L);
        expired.setValidUntil(LocalDate.parse("2025-12-31"));
        when(ranges.findByIdForUpdate(8L, 902091864L)).thenReturn(Optional.of(expired));
        assertThrows(IllegalStateException.class,
                () -> service.allocate(902091864L, 8L, LocalDate.parse("2026-09-04")));
    }

    private DianNumberingRange range(Long id, Long from, Long to, Long current) {
        Empresa company = new Empresa();
        company.setNit(902091864L);
        DianNumberingRange range = new DianNumberingRange();
        range.setId(id);
        range.setEmpresa(company);
        range.setPrefix("SETP");
        range.setResolutionNumber("18760000001");
        range.setRangeFrom(from);
        range.setRangeTo(to);
        range.setCurrentNumber(current);
        range.setValidFrom(LocalDate.parse("2026-01-01"));
        range.setValidUntil(LocalDate.parse("2026-12-31"));
        range.setActive(true);
        return range;
    }
}
