package proyecto.dian.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.dian.dto.DianConfigurationResponse;
import proyecto.dian.dto.DianConfigurationUpdateRequest;
import proyecto.dian.dto.DianConfigurationValidationResponse;
import proyecto.dian.model.DianEnvironment;
import proyecto.dian.service.DianConfigurationService;

@RestController
@RequestMapping("/api/administrador/dian/configuration")
@RequiredArgsConstructor
public class DianConfigurationController {
    private final DianConfigurationService service;

    @GetMapping
    public ResponseEntity<DianConfigurationResponse> get(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "HABILITACION") DianEnvironment environment) {
        return ResponseEntity.ok(service.get(authorization, environment));
    }

    @PutMapping
    public ResponseEntity<DianConfigurationResponse> update(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody DianConfigurationUpdateRequest request) {
        return ResponseEntity.ok(service.update(authorization, request));
    }

    @PostMapping("/validate")
    public ResponseEntity<DianConfigurationValidationResponse> validate(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "HABILITACION") DianEnvironment environment) {
        return ResponseEntity.ok(service.validate(authorization, environment));
    }
}
