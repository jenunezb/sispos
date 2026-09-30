package proyecto.dian.dto;

import jakarta.validation.constraints.NotNull;
import proyecto.dian.model.DianEnvironment;

public record DianDocumentSigningRequest(@NotNull DianEnvironment environment) {}
