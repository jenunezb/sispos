package proyecto.dian.dto;

import proyecto.dian.model.DianEnvironment;

import java.time.LocalDateTime;

public record DianCertificateResponse(
        DianEnvironment environment,
        boolean certificateConfigured,
        LocalDateTime expiration,
        String subject
) {
}
