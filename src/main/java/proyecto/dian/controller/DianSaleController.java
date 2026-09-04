package proyecto.dian.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.dian.dto.DianSaleValidationResponse;
import proyecto.dian.dto.DianDocumentPreparationRequest;
import proyecto.dian.dto.DianDocumentResponse;
import proyecto.dian.service.DianDocumentPreparationService;
import proyecto.dian.service.DianSaleValidationService;

@RestController
@RequestMapping("/api/ventas/{ventaId}/factura-electronica")
@RequiredArgsConstructor
public class DianSaleController {
    private final DianSaleValidationService validation;
    private final DianDocumentPreparationService preparation;

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
}
