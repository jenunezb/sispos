package proyecto.dian.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.dian.dto.DianNumberingRangeRequest;
import proyecto.dian.dto.DianNumberingRangeResponse;
import proyecto.dian.service.DianNumberingService;

import java.util.List;

@RestController
@RequestMapping("/api/administrador/dian/numbering-ranges")
@RequiredArgsConstructor
public class DianNumberingController {
    private final DianNumberingService service;

    @GetMapping
    public ResponseEntity<List<DianNumberingRangeResponse>> list(
            @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(service.list(authorization));
    }

    @PostMapping
    public ResponseEntity<DianNumberingRangeResponse> create(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody DianNumberingRangeRequest request) {
        return ResponseEntity.ok(service.create(authorization, request));
    }
}
