package proyecto.dian.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.dian.dto.DianSaleValidationResponse;
import proyecto.dian.dto.DianDocumentPreparationRequest;
import proyecto.dian.dto.DianDocumentResponse;
import proyecto.dian.dto.DianDocumentSigningRequest;
import proyecto.dian.service.DianDocumentSigningService;
import proyecto.dian.service.DianInvoicePdfService;
import proyecto.dian.model.DianEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import proyecto.dian.service.DianDocumentPreparationService;
import proyecto.dian.service.DianSaleValidationService;
import proyecto.dian.service.DianInvoiceTransmissionService;

@RestController
@RequestMapping("/api/ventas/{ventaId}/factura-electronica")
@RequiredArgsConstructor
public class DianSaleController {
    private final DianSaleValidationService validation;
    private final DianDocumentPreparationService preparation;
    private final DianDocumentSigningService signing;
    private final DianInvoicePdfService invoicePdf;
    private final DianInvoiceTransmissionService transmission;

    @GetMapping("/validate")
    public ResponseEntity<DianSaleValidationResponse> validate(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long ventaId) {
        return ResponseEntity.ok(validation.validate(authorization, ventaId));
    }

    @PostMapping("/prepare")
    public ResponseEntity<DianDocumentResponse> prepare(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long ventaId,
            @Valid @RequestBody DianDocumentPreparationRequest request) {
        return ResponseEntity.ok(preparation.prepare(authorization, ventaId, request));
    }

    @PostMapping("/sign")
    public ResponseEntity<DianDocumentResponse> sign(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long ventaId,
            @Valid @RequestBody DianDocumentSigningRequest request) {
        return ResponseEntity.ok(signing.sign(authorization, ventaId, request.environment()));
    }

    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long ventaId,
            @RequestParam DianEnvironment environment) {
        DianInvoicePdfService.PdfResult result = invoicePdf.generate(authorization, ventaId, environment);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + result.fileName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(result.content());
    }

    @PostMapping("/send-test-set")
    public ResponseEntity<DianDocumentResponse> sendTestSet(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long ventaId) {
        return ResponseEntity.ok(transmission.sendTestSet(authorization, ventaId));
    }

    @PostMapping("/refresh-status")
    public ResponseEntity<DianDocumentResponse> refreshStatus(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long ventaId,
            @RequestParam DianEnvironment environment) {
        return ResponseEntity.ok(transmission.refreshStatus(authorization, ventaId, environment));
    }

    @GetMapping("/status")
    public ResponseEntity<DianDocumentResponse> status(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long ventaId,
            @RequestParam DianEnvironment environment) {
        return ResponseEntity.ok(transmission.get(authorization, ventaId, environment));
    }
}
