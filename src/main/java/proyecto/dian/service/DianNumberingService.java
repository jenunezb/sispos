package proyecto.dian.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import proyecto.dian.dto.DianNumberingRangeRequest;
import proyecto.dian.dto.DianNumberingRangeResponse;
import proyecto.dian.model.DianConfiguration;
import proyecto.dian.model.DianNumberingRange;
import proyecto.dian.repository.DianConfigurationRepository;
import proyecto.dian.repository.DianNumberingRangeRepository;
import proyecto.entidades.Empresa;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DianNumberingService {
    private final DianNumberingRangeRepository ranges;
    private final DianConfigurationRepository configurations;
    private final DianTenantContextService tenantContext;
    private final DianCryptoService crypto;

    @Transactional(readOnly = true)
    public List<DianNumberingRangeResponse> list(String authorization) {
        Empresa company = tenantContext.requireCompanyAdministrator(authorization);
        return ranges.findByEmpresaNitOrderByCreatedAtDesc(company.getNit()).stream()
                .map(DianNumberingRangeResponse::from).toList();
    }

    public DianNumberingRangeResponse create(String authorization, DianNumberingRangeRequest request) {
        Empresa company = tenantContext.requireCompanyAdministrator(authorization);
        validate(request);
        DianConfiguration configuration = configurations
                .findByEmpresaNitAndEnvironment(company.getNit(), request.environment())
                .orElseThrow(() -> new IllegalArgumentException("Primero debe guardar la configuración DIAN del ambiente"));
        String prefix = request.prefix().trim();
        if (ranges.existsByEmpresaNitAndEnvironmentAndDocumentTypeAndPrefixAndActiveTrue(
                company.getNit(), request.environment(), request.documentType(), prefix)) {
            throw new IllegalArgumentException("Ya existe un rango activo para ese ambiente, tipo y prefijo");
        }
        DianNumberingRange range = new DianNumberingRange();
        range.setEmpresa(company);
        range.setConfiguration(configuration);
        range.setEnvironment(request.environment());
        range.setDocumentType(request.documentType());
        range.setPrefix(prefix);
        range.setResolutionNumber(request.resolutionNumber().trim());
        range.setRangeFrom(request.rangeFrom());
        range.setRangeTo(request.rangeTo());
        range.setCurrentNumber(request.rangeFrom() - 1);
        range.setValidFrom(request.validFrom());
        range.setValidUntil(request.validUntil());
        range.setActive(true);
        if (request.technicalKey() != null && !request.technicalKey().isBlank()) {
            range.setTechnicalKeyEncrypted(crypto.encrypt(request.technicalKey(), secretContext(company, request)));
        }
        return DianNumberingRangeResponse.from(ranges.save(range));
    }

    public AllocatedNumber allocate(Long companyNit, Long rangeId, LocalDate issueDate) {
        DianNumberingRange range = ranges.findByIdForUpdate(rangeId, companyNit)
                .orElseThrow(() -> new IllegalArgumentException("Rango de numeración no encontrado para la empresa"));
        if (!Boolean.TRUE.equals(range.getActive())) throw new IllegalStateException("El rango de numeración está inactivo");
        if (issueDate.isBefore(range.getValidFrom()) || issueDate.isAfter(range.getValidUntil())) {
            throw new IllegalStateException("El rango de numeración no está vigente");
        }
        long next = Math.addExact(range.getCurrentNumber(), 1L);
        if (next < range.getRangeFrom() || next > range.getRangeTo()) {
            throw new IllegalStateException("El rango de numeración está agotado");
        }
        range.setCurrentNumber(next);
        ranges.save(range);
        return new AllocatedNumber(range.getId(), range.getPrefix(), next, range.getPrefix() + next,
                range.getResolutionNumber(), range.getRangeFrom(), range.getRangeTo(),
                range.getValidFrom(), range.getValidUntil());
    }

    private void validate(DianNumberingRangeRequest request) {
        if (request.rangeFrom() > request.rangeTo()) throw new IllegalArgumentException("El rango inicial supera el final");
        if (request.validFrom().isAfter(request.validUntil())) throw new IllegalArgumentException("La vigencia inicial supera la final");
        if (request.prefix().trim().length() > 20) throw new IllegalArgumentException("El prefijo supera 20 caracteres");
    }

    private String secretContext(Empresa company, DianNumberingRangeRequest request) {
        return "dian:" + company.getNit() + ":" + request.environment() + ":range:"
                + request.documentType() + ":" + request.prefix().trim();
    }

    public record AllocatedNumber(Long rangeId, String prefix, long consecutive, String fullNumber,
                                  String resolutionNumber, long rangeFrom, long rangeTo,
                                  LocalDate validFrom, LocalDate validUntil) {}
}
