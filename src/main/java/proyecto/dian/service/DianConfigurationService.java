package proyecto.dian.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import proyecto.dian.dto.DianConfigurationResponse;
import proyecto.dian.dto.DianConfigurationUpdateRequest;
import proyecto.dian.dto.DianConfigurationValidationResponse;
import proyecto.dian.model.*;
import proyecto.dian.repository.DianConfigurationRepository;
import proyecto.dian.repository.DianTransmissionAttemptRepository;
import proyecto.entidades.Empresa;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DianConfigurationService {
    private final DianConfigurationRepository configurations;
    private final DianTransmissionAttemptRepository audit;
    private final DianTenantContextService tenantContext;
    private final DianCryptoService crypto;

    @Transactional(readOnly = true)
    public DianConfigurationResponse get(String authorization, DianEnvironment environment) {
        Empresa company = authenticatedCompany(authorization);
        return configurations.findByEmpresaNitAndEnvironment(company.getNit(), environment)
                .map(DianConfigurationResponse::from)
                .orElseGet(() -> empty(environment));
    }

    public DianConfigurationResponse update(String authorization, DianConfigurationUpdateRequest request) {
        Empresa company = authenticatedCompany(authorization);
        DianConfiguration configuration = configurations
                .findByEmpresaNitAndEnvironment(company.getNit(), request.environment())
                .orElseGet(() -> newConfiguration(company, request.environment()));

        configuration.setOperationMode(request.operationMode() == null
                ? DianOperationMode.SOFTWARE_PROPIO : request.operationMode());
        configuration.setSoftwareId(clean(request.softwareId()));
        configuration.setTestSetId(clean(request.testSetId()));
        configuration.setTestPrefix(clean(request.testPrefix()));
        configuration.setTestRangeFrom(request.testRangeFrom());
        configuration.setTestRangeTo(request.testRangeTo());
        configuration.setTestResolutionNumber(clean(request.testResolutionNumber()));
        configuration.setTestValidFrom(request.testValidFrom());
        configuration.setTestValidUntil(request.testValidUntil());

        validateRanges(request);
        String context = secretContext(company.getNit(), request.environment());
        if (request.softwarePin() != null) {
            configuration.setSoftwarePinEncrypted(crypto.encrypt(requireSecret(request.softwarePin(), "Software PIN"), context + ":pin"));
        }
        if (request.technicalKey() != null) {
            configuration.setTechnicalKeyEncrypted(crypto.encrypt(requireSecret(request.technicalKey(), "clave tecnica"), context + ":technical-key"));
        }
        configuration.setStatus(hasMinimumConfiguration(configuration)
                ? DianConfigurationStatus.CONFIGURED : DianConfigurationStatus.NOT_CONFIGURED);
        DianConfiguration saved = configurations.save(configuration);
        recordAudit(company, "CONFIGURATION_UPDATE", "SUCCESS", null, null);
        return DianConfigurationResponse.from(saved);
    }

    public DianConfigurationValidationResponse validate(String authorization, DianEnvironment environment) {
        Empresa company = authenticatedCompany(authorization);
        DianConfiguration configuration = configurations.findByEmpresaNitAndEnvironment(company.getNit(), environment)
                .orElse(null);
        List<String> missing = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        if (configuration == null) {
            missing.add("configuration");
        } else {
            required(missing, "softwareId", configuration.getSoftwareId());
            required(missing, "softwarePin", configuration.getSoftwarePinEncrypted());
            required(missing, "certificate", configuration.getCertificateReference());
            if (environment == DianEnvironment.HABILITACION) {
                required(missing, "testSetId", configuration.getTestSetId());
                required(missing, "testPrefix", configuration.getTestPrefix());
                required(missing, "testResolutionNumber", configuration.getTestResolutionNumber());
                if (configuration.getTestRangeFrom() == null) missing.add("testRangeFrom");
                if (configuration.getTestRangeTo() == null) missing.add("testRangeTo");
            }
            verifyEncryptedSecret(configuration.getSoftwarePinEncrypted(),
                    secretContext(company.getNit(), environment) + ":pin", "softwarePin", errors);
            verifyEncryptedSecret(configuration.getTechnicalKeyEncrypted(),
                    secretContext(company.getNit(), environment) + ":technical-key", "technicalKey", errors);
        }
        boolean valid = missing.isEmpty() && errors.isEmpty();
        recordAudit(company, "CONFIGURATION_VALIDATE", valid ? "SUCCESS" : "INVALID", null,
                valid ? null : DianErrorCategory.CONFIGURATION_ERROR);
        return new DianConfigurationValidationResponse(environment, valid, List.copyOf(missing), List.copyOf(errors));
    }

    private Empresa authenticatedCompany(String authorization) {
        return tenantContext.requireCompanyAdministrator(authorization);
    }

    private DianConfiguration newConfiguration(Empresa company, DianEnvironment environment) {
        DianConfiguration value = new DianConfiguration();
        value.setEmpresa(company);
        value.setEnvironment(environment);
        value.setOperationMode(DianOperationMode.SOFTWARE_PROPIO);
        value.setStatus(DianConfigurationStatus.NOT_CONFIGURED);
        return value;
    }

    private DianConfigurationResponse empty(DianEnvironment environment) {
        return new DianConfigurationResponse(null, environment, DianOperationMode.SOFTWARE_PROPIO,
                null, false, null, false, null, null, null, null, null, null,
                false, null, DianConfigurationStatus.NOT_CONFIGURED, null);
    }

    private void validateRanges(DianConfigurationUpdateRequest request) {
        if (request.testRangeFrom() != null && request.testRangeTo() != null
                && request.testRangeFrom() > request.testRangeTo()) {
            throw new IllegalArgumentException("El inicio del rango de prueba no puede superar el final");
        }
        if (request.testValidFrom() != null && request.testValidUntil() != null
                && request.testValidFrom().isAfter(request.testValidUntil())) {
            throw new IllegalArgumentException("La fecha inicial de prueba no puede superar la fecha final");
        }
    }

    private boolean hasMinimumConfiguration(DianConfiguration value) {
        return hasText(value.getSoftwareId()) && hasText(value.getSoftwarePinEncrypted());
    }

    private void verifyEncryptedSecret(String encrypted, String context, String field, List<String> errors) {
        if (!hasText(encrypted)) return;
        try {
            crypto.decrypt(encrypted, context);
        } catch (RuntimeException ignored) {
            errors.add("No fue posible validar el secreto almacenado: " + field);
        }
    }

    private void recordAudit(Empresa company, String operation, String status, String sanitizedMessage,
                             DianErrorCategory category) {
        DianTransmissionAttempt attempt = new DianTransmissionAttempt();
        attempt.setEmpresa(company);
        attempt.setOperation(operation);
        attempt.setCorrelationId(UUID.randomUUID().toString());
        attempt.setStatus(status);
        attempt.setErrorCategory(category);
        attempt.setErrorMessageSanitized(sanitizedMessage);
        audit.save(attempt);
    }

    private String secretContext(Long companyNit, DianEnvironment environment) {
        return "dian:" + companyNit + ":" + environment.name();
    }

    private static String requireSecret(String value, String name) {
        if (value.isBlank()) throw new IllegalArgumentException("El " + name + " no puede estar vacio");
        return value;
    }

    private static void required(List<String> missing, String field, String value) {
        if (!hasText(value)) missing.add(field);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String clean(String value) {
        if (value == null) return null;
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
