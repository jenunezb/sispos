package proyecto.dian.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import proyecto.dian.dto.DianDocumentResponse;
import proyecto.dian.model.*;
import proyecto.dian.repository.DianConfigurationRepository;
import proyecto.dian.repository.DianElectronicDocumentRepository;
import proyecto.entidades.Empresa;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DianInvoiceTransmissionServiceTest {
    private final DianTenantContextService tenant = mock(DianTenantContextService.class);
    private final DianElectronicDocumentRepository documents = mock(DianElectronicDocumentRepository.class);
    private final DianConfigurationRepository configurations = mock(DianConfigurationRepository.class);
    private final DianTransmissionPackageService packages = mock(DianTransmissionPackageService.class);
    private final DianHttpSoapTransport transport = mock(DianHttpSoapTransport.class);
    private final DianSoapMessageService soap = mock(DianSoapMessageService.class);
    private final DianPrivateStorageService storage = mock(DianPrivateStorageService.class);
    private final DianTransmissionStateService states = mock(DianTransmissionStateService.class);
    private final DianInvoiceTransmissionService service = new DianInvoiceTransmissionService(
            tenant, documents, configurations, packages, transport, soap, storage, states);
    private Empresa company;
    private DianConfiguration configuration;

    @BeforeEach
    void setUp() {
        company = new Empresa();
        company.setNit(902091864L);
        configuration = new DianConfiguration();
        configuration.setEmpresa(company);
        configuration.setEnvironment(DianEnvironment.HABILITACION);
        when(tenant.requireCompanyAdministrator("Bearer token")).thenReturn(company);
        when(configurations.findByEmpresaNitAndEnvironment(company.getNit(), DianEnvironment.HABILITACION))
                .thenReturn(Optional.of(configuration));
        when(packages.serviceUrl(configuration)).thenReturn(DianTransmissionPackageService.TEST_SERVICE_URL);
    }

    @Test
    void sendsSignedInvoiceAndStoresTrackId() {
        DianElectronicDocument document = document(DianDocumentStatus.SIGNED);
        find(document);
        byte[] envelope = "<secured/>".getBytes(StandardCharsets.UTF_8);
        byte[] response = "<response/>".getBytes(StandardCharsets.UTF_8);
        when(packages.prepareSecuredTestSet(document, configuration)).thenReturn(
                new DianTransmissionPackageService.PackageData("invoice.xml", "invoice.zip", new byte[]{1}, envelope));
        when(transport.exchange(DianTransmissionPackageService.TEST_SERVICE_URL,
                DianTransmissionPackageService.SEND_TEST_SET_ACTION, envelope))
                .thenReturn(new DianHttpSoapTransport.Response(200, response));
        when(storage.storeXml(company.getNit(), document.getId(), "response", response))
                .thenReturn("902091864/10/response.xml");
        when(soap.parseResponse(response)).thenReturn(new DianSoapMessageService.DianSoapResult(
                "track-123", false, null, "Recibido", List.of()));
        when(states.sent(document, "track-123", "Recibido")).thenAnswer(call -> {
            document.setTrackId("track-123");
            document.setStatus(DianDocumentStatus.SENT);
            return document;
        });

        DianDocumentResponse result = service.sendTestSet("Bearer token", 55L);

        assertEquals(DianDocumentStatus.SENT, result.status());
        assertEquals("track-123", result.trackId());
        verify(states).sent(document, "track-123", "Recibido");
    }

    @Test
    void preventsDuplicateSend() {
        DianElectronicDocument document = document(DianDocumentStatus.SENT);
        find(document);
        assertThrows(IllegalStateException.class, () -> service.sendTestSet("Bearer token", 55L));
        verifyNoInteractions(transport);
    }

    @Test
    void refreshesAcceptedStatus() {
        DianElectronicDocument document = document(DianDocumentStatus.SENT);
        document.setTrackId("track-123");
        find(document);
        byte[] envelope = "<status/>".getBytes(StandardCharsets.UTF_8);
        byte[] response = "<accepted/>".getBytes(StandardCharsets.UTF_8);
        when(packages.prepareSecuredStatusQuery(configuration, "track-123")).thenReturn(envelope);
        when(transport.exchange(DianTransmissionPackageService.TEST_SERVICE_URL,
                DianTransmissionPackageService.GET_STATUS_ZIP_ACTION, envelope))
                .thenReturn(new DianHttpSoapTransport.Response(200, response));
        when(storage.storeXml(company.getNit(), document.getId(), "response", response))
                .thenReturn("902091864/10/response.xml");
        var parsed = new DianSoapMessageService.DianSoapResult(null, true, "00",
                "Procesado correctamente", List.of());
        when(soap.parseResponse(response)).thenReturn(parsed);
        when(states.resolved(document, parsed)).thenAnswer(call -> {
            document.setStatus(DianDocumentStatus.ACCEPTED);
            document.setDianStatusCode("00");
            return document;
        });

        DianDocumentResponse result = service.refreshStatus("Bearer token", 55L,
                DianEnvironment.HABILITACION);

        assertEquals(DianDocumentStatus.ACCEPTED, result.status());
        assertEquals("00", result.dianStatusCode());
    }

    @Test
    void rejectsUnexpectedHttpResponseWithoutChangingState() {
        DianElectronicDocument document = document(DianDocumentStatus.SIGNED);
        find(document);
        byte[] envelope = "<secured/>".getBytes(StandardCharsets.UTF_8);
        when(packages.prepareSecuredTestSet(document, configuration)).thenReturn(
                new DianTransmissionPackageService.PackageData("invoice.xml", "invoice.zip", new byte[]{1}, envelope));
        when(transport.exchange(anyString(), anyString(), eq(envelope)))
                .thenReturn(new DianHttpSoapTransport.Response(503, new byte[]{1}));

        assertThrows(IllegalStateException.class, () -> service.sendTestSet("Bearer token", 55L));
        verifyNoInteractions(states);
        verifyNoInteractions(storage);
    }

    private void find(DianElectronicDocument document) {
        when(documents.findByEmpresaNitAndEnvironmentAndVentaIdAndDocumentType(company.getNit(),
                DianEnvironment.HABILITACION, 55L, DianDocumentType.INVOICE)).thenReturn(Optional.of(document));
    }

    private DianElectronicDocument document(DianDocumentStatus status) {
        DianElectronicDocument document = new DianElectronicDocument();
        document.setId(10L);
        document.setEmpresa(company);
        document.setEnvironment(DianEnvironment.HABILITACION);
        document.setDocumentType(DianDocumentType.INVOICE);
        document.setStatus(status);
        document.setAttempts(0);
        return document;
    }
}
