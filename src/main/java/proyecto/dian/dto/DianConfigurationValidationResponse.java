package proyecto.dian.dto;

import proyecto.dian.model.DianEnvironment;

import java.util.List;

public record DianConfigurationValidationResponse(
        DianEnvironment environment,
        boolean valid,
        List<String> missingFields,
        List<String> errors
) {
}
