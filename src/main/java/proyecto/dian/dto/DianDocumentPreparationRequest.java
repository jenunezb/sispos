package proyecto.dian.dto;

import jakarta.validation.constraints.NotNull;
import proyecto.dian.model.DianEnvironment;

public record DianDocumentPreparationRequest(
        @NotNull DianEnvironment environment,
        @NotNull Long numberingRangeId
) {}
