package proyecto.dian.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import proyecto.dian.dto.DianDocumentPreparationRequest;
import proyecto.dian.dto.DianDocumentResponse;
import proyecto.dian.dto.DianSaleValidationResponse;
import proyecto.dian.model.*;
import proyecto.dian.repository.DianConfigurationRepository;
import proyecto.dian.repository.DianElectronicDocumentRepository;
import proyecto.entidades.Empresa;
import proyecto.entidades.Venta;
import proyecto.repositorios.VentaRepository;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class DianDocumentPreparationService {
    private final DianTenantContextService tenantContext;
    private final DianSaleValidationService saleValidation;
    private final DianConfigurationRepository configurations;
    private final DianElectronicDocumentRepository documents;
    private final DianNumberingService numbering;
    private final VentaRepository sales;

    public DianDocumentResponse prepare(String authorization, Long saleId, DianDocumentPreparationRequest request) {
        Empresa company = tenantContext.requireCompanyAdministrator(authorization);
        Venta sale = sales.findByIdAndSedeEmpresaNit(saleId, company.getNit())
                .orElseThrow(() -> new IllegalArgumentException("Venta no encontrada para la empresa autenticada"));

        return documents.findByEmpresaNitAndEnvironmentAndVentaIdAndDocumentType(
                        company.getNit(), request.environment(), saleId, DianDocumentType.INVOICE)
                .map(DianDocumentResponse::from)
                .orElseGet(() -> createDraft(company, sale, request));
    }

    private DianDocumentResponse createDraft(Empresa company, Venta sale, DianDocumentPreparationRequest request) {
        DianSaleValidationResponse validation = saleValidation.validate(sale);
        if (!validation.ready()) {
            throw new IllegalArgumentException("La venta no está lista para DIAN. Faltan: "
                    + String.join(", ", validation.missingFields()));
        }
        DianConfiguration configuration = configurations
                .findByEmpresaNitAndEnvironment(company.getNit(), request.environment())
                .orElseThrow(() -> new IllegalArgumentException("No existe configuración DIAN para el ambiente"));
        if (configuration.getStatus() == DianConfigurationStatus.NOT_CONFIGURED) {
            throw new IllegalStateException("La configuración DIAN no está completa");
        }

        LocalDate issueDate = sale.getFecha().toLocalDate();
        DianNumberingService.AllocatedNumber allocated =
                numbering.allocate(company.getNit(), request.numberingRangeId(), issueDate);
        DianElectronicDocument document = new DianElectronicDocument();
        document.setEmpresa(company);
        document.setVenta(sale);
        document.setDocumentType(DianDocumentType.INVOICE);
        document.setEnvironment(request.environment());
        document.setPrefix(allocated.prefix());
        document.setConsecutive(allocated.consecutive());
        document.setFullNumber(allocated.fullNumber());
        document.setTestSetId(request.environment() == DianEnvironment.HABILITACION
                ? configuration.getTestSetId() : null);
        document.setStatus(DianDocumentStatus.DRAFT);
        document.setAttempts(0);
        return DianDocumentResponse.from(documents.save(document));
    }
}
