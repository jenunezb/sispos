package proyecto.dian.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import proyecto.dian.model.DianDocumentType;
import proyecto.dian.model.DianEnvironment;

import java.time.LocalDate;

public record DianNumberingRangeRequest(
        @NotNull DianEnvironment environment,
        @NotNull DianDocumentType documentType,
        @NotBlank String prefix,
        @NotBlank String resolutionNumber,
        @NotNull @Positive Long rangeFrom,
        @NotNull @Positive Long rangeTo,
        @NotNull LocalDate validFrom,
        @NotNull LocalDate validUntil,
        String technicalKey
) {}
