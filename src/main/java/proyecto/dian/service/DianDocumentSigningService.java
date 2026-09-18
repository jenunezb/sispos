package proyecto.dian.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import proyecto.dian.dto.DianDocumentResponse;
import proyecto.dian.model.*;
import proyecto.dian.repository.DianConfigurationRepository;
import proyecto.dian.repository.DianElectronicDocumentRepository;
import proyecto.entidades.Empresa;

@Service
@RequiredArgsConstructor
@Transactional
public class DianDocumentSigningService {
    private final DianTenantContextService tenantContext;
    private final DianElectronicDocumentRepository documents;
    private final DianConfigurationRepository configurations;
    private final DianCertificateService certificates;
    private final DianPrivateStorageService storage;
    private final DianXadesSigner signer;

    public DianDocumentResponse sign(String authorization, Long saleId, DianEnvironment environment) {
        Empresa company = tenantContext.requireCompanyAdministrator(authorization);
        DianElectronicDocument document = documents.findByEmpresaNitAndEnvironmentAndVentaIdAndDocumentType(
                        company.getNit(), environment, saleId, DianDocumentType.INVOICE)
                .orElseThrow(() -> new IllegalArgumentException("Primero debe preparar la factura electrónica"));
        if (document.getStatus() == DianDocumentStatus.SIGNED) return DianDocumentResponse.from(document);
        if (document.getStatus() != DianDocumentStatus.GENERATED) {
            throw new IllegalStateException("Solo se puede firmar una factura en estado GENERATED");
        }
        DianConfiguration configuration = configurations.findByEmpresaNitAndEnvironment(company.getNit(), environment)
                .orElseThrow(() -> new IllegalArgumentException("No existe configuración DIAN para el ambiente"));
        DianCertificateService.SigningMaterial material = certificates.load(configuration);
        byte[] unsignedXml = storage.readXml(company.getNit(), document.getId(), "request");
        byte[] signedXml = signer.sign(unsignedXml, material.pkcs12(), material.password());
        document.setSignedXmlStorageReference(
                storage.storeXml(company.getNit(), document.getId(), "signed", signedXml));
        document.setStatus(DianDocumentStatus.SIGNED);
        return DianDocumentResponse.from(documents.save(document));
    }
}
