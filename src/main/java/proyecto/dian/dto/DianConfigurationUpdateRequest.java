package proyecto.dian.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import proyecto.dian.model.DianEnvironment;
import proyecto.dian.model.DianOperationMode;

import java.time.LocalDate;

public record DianConfigurationUpdateRequest(
        @NotNull DianEnvironment environment,
        DianOperationMode operationMode,
        @Size(max = 100) String softwareId,
        @Size(max = 200) String softwarePin,
        @Size(max = 100) String testSetId,
        @Size(max = 500) String technicalKey,
        @Pattern(regexp = "^[A-Za-z0-9_-]{1,20}$", message = "El prefijo de prueba no es valido") String testPrefix,
        Long testRangeFrom,
        Long testRangeTo,
        @Size(max = 100) String testResolutionNumber,
        LocalDate testValidFrom,
        LocalDate testValidUntil
) {
    @Override
    public String toString() {
        return "DianConfigurationUpdateRequest[environment=" + environment
                + ", operationMode=" + operationMode
                + ", softwareId=" + softwareId
                + ", softwarePin=***"
                + ", testSetId=" + testSetId
                + ", technicalKey=***"
                + ", testPrefix=" + testPrefix
                + ", testRangeFrom=" + testRangeFrom
                + ", testRangeTo=" + testRangeTo
                + ", testResolutionNumber=" + testResolutionNumber
                + ", testValidFrom=" + testValidFrom
                + ", testValidUntil=" + testValidUntil + "]";
    }
}
