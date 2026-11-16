package proyecto.dian.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import proyecto.dian.dto.DianDocumentResponse;
import proyecto.dian.model.*;
import proyecto.dian.repository.DianConfigurationRepository;
import proyecto.dian.repository.DianElectronicDocumentRepository;
import proyecto.entidades.Empresa;

@Service
@RequiredArgsConstructor
@Transactional
public class DianInvoiceTransmissionService {
    private final DianTenantContextService tenantContext;
    private final DianElectronicDocumentRepository documents;
    private final DianConfigurationRepository configurations;
    private final DianTransmissionPackageService packages;
    private final DianHttpSoapTransport transport;
    private final DianSoapMessageService soap;
    private final DianPrivateStorageService storage;
    private final DianTransmissionStateService states;

    public DianDocumentResponse sendTestSet(String authorization, Long saleId) {
        Context context = context(authorization, saleId, DianEnvironment.HABILITACION);
        DianElectronicDocument document = context.document();
        if (document.getStatus() != DianDocumentStatus.SIGNED) {
            throw new IllegalStateException("La factura debe estar firmada y sin envíos previos");
        }
        DianTransmissionPackageService.PackageData request =
                packages.prepareSecuredTestSet(document, context.configuration());
        DianHttpSoapTransport.Response response = transport.exchange(packages.serviceUrl(context.configuration()),
                DianTransmissionPackageService.SEND_TEST_SET_ACTION, request.soapEnvelope());
        requireSuccessfulHttp(response);
        storeResponse(document, response.body());
        DianSoapMessageService.DianSoapResult result = soap.parseResponse(response.body());
        String trackId = result.zipKey();
        if (trackId == null || trackId.isBlank()) {
            throw new IllegalStateException(message(result, "La DIAN no devolvió el TrackId del set de pruebas"));
        }
        return DianDocumentResponse.from(states.sent(document, trackId, summary(result)));
    }

    @Transactional(readOnly = true)
    public DianDocumentResponse get(String authorization, Long saleId, DianEnvironment environment) {
        return DianDocumentResponse.from(context(authorization, saleId, environment).document());
    }

    public DianDocumentResponse refreshStatus(String authorization, Long saleId, DianEnvironment environment) {
        Context context = context(authorization, saleId, environment);
        DianElectronicDocument document = context.document();
        if (document.getStatus() == DianDocumentStatus.ACCEPTED
                || document.getStatus() == DianDocumentStatus.REJECTED) {
            return DianDocumentResponse.from(document);
        }
        if (document.getStatus() != DianDocumentStatus.SENT
                && document.getStatus() != DianDocumentStatus.PROCESSING) {
            throw new IllegalStateException("La factura todavía no tiene un envío pendiente en la DIAN");
        }
        byte[] request = packages.prepareSecuredStatusQuery(context.configuration(), document.getTrackId());
        DianHttpSoapTransport.Response response = transport.exchange(packages.serviceUrl(context.configuration()),
                DianTransmissionPackageService.GET_STATUS_ZIP_ACTION, request);
        requireSuccessfulHttp(response);
        storeResponse(document, response.body());
        DianSoapMessageService.DianSoapResult result = soap.parseResponse(response.body());
        if (isProcessing(result)) {
            return DianDocumentResponse.from(states.processing(document, result.statusDescription()));
        }
        return DianDocumentResponse.from(states.resolved(document, result));
    }

    private Context context(String authorization, Long saleId, DianEnvironment environment) {
        Empresa company = tenantContext.requireCompanyAdministrator(authorization);
        DianElectronicDocument document = documents.findByEmpresaNitAndEnvironmentAndVentaIdAndDocumentType(
                        company.getNit(), environment, saleId, DianDocumentType.INVOICE)
                .orElseThrow(() -> new IllegalArgumentException("Factura electrónica no encontrada para la empresa"));
        DianConfiguration configuration = configurations.findByEmpresaNitAndEnvironment(company.getNit(), environment)
                .orElseThrow(() -> new IllegalArgumentException("No existe configuración DIAN para el ambiente"));
        return new Context(document, configuration);
    }

    private boolean isProcessing(DianSoapMessageService.DianSoapResult result) {
        if (result.valid()) return false;
        String code = result.statusCode();
        String description = result.statusDescription();
        return "66".equals(code) || (description != null
                && description.toLowerCase(java.util.Locale.ROOT).contains("proceso"));
    }

    private void requireSuccessfulHttp(DianHttpSoapTransport.Response response) {
        if (response.httpStatus() < 200 || response.httpStatus() >= 300) {
            throw new IllegalStateException("La DIAN respondió HTTP " + response.httpStatus());
        }
    }

    private void storeResponse(DianElectronicDocument document, byte[] response) {
        document.setResponseXmlStorageReference(storage.storeXml(document.getEmpresa().getNit(),
                document.getId(), "response", response));
    }

    private String summary(DianSoapMessageService.DianSoapResult result) {
        return message(result, "Solicitud recibida por la DIAN");
    }

    private String message(DianSoapMessageService.DianSoapResult result, String fallback) {
        if (result.statusDescription() != null) return result.statusDescription();
        if (!result.errors().isEmpty()) return String.join("; ", result.errors());
        return fallback;
    }

    private record Context(DianElectronicDocument document, DianConfiguration configuration) {}
}
