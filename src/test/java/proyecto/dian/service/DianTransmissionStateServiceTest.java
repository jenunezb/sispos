package proyecto.dian.service;

import org.junit.jupiter.api.Test;
import proyecto.dian.model.*;
import proyecto.dian.repository.DianElectronicDocumentRepository;
import proyecto.dian.repository.DianTransmissionAttemptRepository;
import proyecto.entidades.Empresa;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DianTransmissionStateServiceTest {
    private final DianElectronicDocumentRepository documents = mock(DianElectronicDocumentRepository.class);
    private final DianTransmissionAttemptRepository attempts = mock(DianTransmissionAttemptRepository.class);
    private final DianTransmissionStateService service = new DianTransmissionStateService(documents, attempts);

    @Test
    void followsSignedSentProcessingAcceptedLifecycle() {
        DianElectronicDocument document = document(DianDocumentStatus.SIGNED);
        when(documents.save(any())).thenAnswer(call -> call.getArgument(0));
        service.sent(document, "track-1", "recibido");
        assertEquals(DianDocumentStatus.SENT, document.getStatus());
        assertEquals(1, document.getAttempts());
        service.processing(document, "en proceso");
        assertEquals(DianDocumentStatus.PROCESSING, document.getStatus());
        service.resolved(document, new DianSoapMessageService.DianSoapResult(
                "track-1", true, "00", "Documento validado", List.of()));
        assertEquals(DianDocumentStatus.ACCEPTED, document.getStatus());
        assertNotNull(document.getValidatedAt());
        verify(attempts, times(3)).save(any());
    }

    @Test
    void storesRejectionWithoutPermittingInvalidTransition() {
        DianElectronicDocument document = document(DianDocumentStatus.SIGNED);
        assertThrows(IllegalStateException.class, () -> service.resolved(document,
                new DianSoapMessageService.DianSoapResult(null, false, "90", "Rechazado", List.of("regla"))));
        verifyNoInteractions(attempts);
    }

    private DianElectronicDocument document(DianDocumentStatus status) {
        Empresa company = new Empresa(); company.setNit(902091864L);
        DianElectronicDocument document = new DianElectronicDocument();
        document.setEmpresa(company); document.setStatus(status); document.setAttempts(0);
        return document;
    }
}
