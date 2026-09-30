package proyecto.dian.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import proyecto.dian.model.*;
import proyecto.dian.repository.DianElectronicDocumentRepository;
import proyecto.dian.repository.DianTransmissionAttemptRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DianTransmissionStateService {
    private final DianElectronicDocumentRepository documents;
    private final DianTransmissionAttemptRepository attempts;

    public DianElectronicDocument sent(DianElectronicDocument document, String trackId, String sanitizedResponse) {
        requireStatus(document, DianDocumentStatus.SIGNED);
        if (trackId == null || trackId.isBlank()) throw new IllegalArgumentException("La DIAN no devolvió TrackId");
        document.setTrackId(trackId.trim());
        document.setStatus(DianDocumentStatus.SENT);
        document.setSentAt(LocalDateTime.now());
        document.setAttempts(document.getAttempts() + 1);
        audit(document, "SEND_TEST_SET_ASYNC", "SENT", sanitizedResponse, null, null);
        return documents.save(document);
    }

    public DianElectronicDocument processing(DianElectronicDocument document, String message) {
        if (document.getStatus() != DianDocumentStatus.SENT
                && document.getStatus() != DianDocumentStatus.PROCESSING) {
            throw new IllegalStateException("La factura no está pendiente de respuesta DIAN");
        }
        document.setStatus(DianDocumentStatus.PROCESSING);
        document.setDianStatusMessage(clean(message));
        audit(document, "GET_STATUS_ZIP", "PROCESSING", message, null, null);
        return documents.save(document);
    }

    public DianElectronicDocument resolved(DianElectronicDocument document,
                                           DianSoapMessageService.DianSoapResult result) {
        if (document.getStatus() != DianDocumentStatus.SENT
                && document.getStatus() != DianDocumentStatus.PROCESSING) {
            throw new IllegalStateException("La factura no está pendiente de respuesta DIAN");
        }
        document.setDianStatusCode(clean(result.statusCode()));
        document.setDianStatusMessage(clean(result.statusDescription()));
        document.setDianValidationErrors(result.errors().isEmpty() ? null : String.join("\n", result.errors()));
        document.setStatus(result.valid() ? DianDocumentStatus.ACCEPTED : DianDocumentStatus.REJECTED);
        document.setValidatedAt(LocalDateTime.now());
        audit(document, "GET_STATUS_ZIP", result.valid() ? "ACCEPTED" : "REJECTED",
                result.statusDescription(), result.valid() ? null : DianErrorCategory.DIAN_REJECTION,
                result.valid() ? null : result.statusDescription());
        return documents.save(document);
    }

    private void requireStatus(DianElectronicDocument document, DianDocumentStatus expected) {
        if (document == null || document.getStatus() != expected) {
            throw new IllegalStateException("La factura debe estar en estado " + expected);
        }
    }

    private void audit(DianElectronicDocument document, String operation, String status, String response,
                       DianErrorCategory category, String error) {
        DianTransmissionAttempt attempt = new DianTransmissionAttempt();
        attempt.setEmpresa(document.getEmpresa());
        attempt.setElectronicDocument(document);
        attempt.setOperation(operation);
        attempt.setCorrelationId(UUID.randomUUID().toString());
        attempt.setStatus(status);
        attempt.setSanitizedResponse(limit(response));
        attempt.setErrorCategory(category);
        attempt.setErrorMessageSanitized(limit(error));
        attempts.save(attempt);
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String limit(String value) {
        String cleaned = clean(value);
        return cleaned == null || cleaned.length() <= 2000 ? cleaned : cleaned.substring(0, 2000);
    }
}
