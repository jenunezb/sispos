package proyecto.dian.dto;

import proyecto.dian.model.DianConfiguration;
import proyecto.dian.model.DianConfigurationStatus;
import proyecto.dian.model.DianEnvironment;
import proyecto.dian.model.DianOperationMode;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DianConfigurationResponse(
        Long id,
        DianEnvironment environment,
        DianOperationMode operationMode,
        String softwareId,
        boolean softwarePinConfigured,
        String testSetId,
        boolean technicalKeyConfigured,
        String testPrefix,
        Long testRangeFrom,
        Long testRangeTo,
        String testResolutionNumber,
        LocalDate testValidFrom,
        LocalDate testValidUntil,
        boolean certificateConfigured,
        LocalDateTime certificateExpiration,
        DianConfigurationStatus status,
        LocalDateTime updatedAt
) {
    public static DianConfigurationResponse from(DianConfiguration value) {
        return new DianConfigurationResponse(value.getId(), value.getEnvironment(), value.getOperationMode(),
                value.getSoftwareId(), hasText(value.getSoftwarePinEncrypted()), value.getTestSetId(),
                hasText(value.getTechnicalKeyEncrypted()), value.getTestPrefix(), value.getTestRangeFrom(),
                value.getTestRangeTo(), value.getTestResolutionNumber(), value.getTestValidFrom(),
                value.getTestValidUntil(), hasText(value.getCertificateReference()), value.getCertificateExpiration(),
                value.getStatus(), value.getUpdatedAt());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
