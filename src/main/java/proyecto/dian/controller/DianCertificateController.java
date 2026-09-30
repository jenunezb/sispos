package proyecto.dian.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import proyecto.dian.dto.DianCertificateResponse;
import proyecto.dian.model.DianEnvironment;
import proyecto.dian.service.DianCertificateService;

@RestController
@RequestMapping("/api/administrador/dian/certificate")
@RequiredArgsConstructor
public class DianCertificateController {
    private final DianCertificateService service;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<DianCertificateResponse> upload(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "HABILITACION") DianEnvironment environment,
            @RequestParam("file") MultipartFile file,
            @RequestParam("password") String password) {
        return ResponseEntity.ok(service.upload(authorization, environment, file, password));
    }

    @DeleteMapping
    public ResponseEntity<DianCertificateResponse> delete(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "HABILITACION") DianEnvironment environment) {
        return ResponseEntity.ok(service.delete(authorization, environment));
    }
}
