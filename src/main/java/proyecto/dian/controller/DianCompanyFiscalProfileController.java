package proyecto.dian.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.dian.dto.DianCompanyFiscalProfile;
import proyecto.dian.service.DianCompanyFiscalProfileService;

@RestController
@RequestMapping("/api/administrador/dian/company-fiscal-profile")
@RequiredArgsConstructor
public class DianCompanyFiscalProfileController {
    private final DianCompanyFiscalProfileService service;

    @GetMapping
    public ResponseEntity<DianCompanyFiscalProfile> get(
            @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(service.get(authorization));
    }

    @PutMapping
    public ResponseEntity<DianCompanyFiscalProfile> update(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody DianCompanyFiscalProfile request) {
        return ResponseEntity.ok(service.update(authorization, request));
    }
}
