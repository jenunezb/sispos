package proyecto.dian.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import proyecto.dian.model.DianConfiguration;
import proyecto.dian.model.DianElectronicDocument;
import proyecto.dian.model.DianEnvironment;

@Service
@RequiredArgsConstructor
public class DianTransmissionPackageService {
    private final DianPrivateStorageService storage;
    private final DianZipService zip;
    private final DianFileNameService names;
    private final DianSoapMessageService soap;
    private final DianCertificateService certificates;
    private final DianWsSecuritySigner wsSecurity;

    static final String TEST_SERVICE_URL = "https://vpfe-hab.dian.gov.co/WcfDianCustomerServices.svc";
    static final String PRODUCTION_SERVICE_URL = "https://vpfe.dian.gov.co/WcfDianCustomerServices.svc";
    static final String SEND_TEST_SET_ACTION =
            "http://wcf.dian.colombia/IWcfDianCustomerServices/SendTestSetAsync";
    static final String GET_STATUS_ZIP_ACTION =
            "http://wcf.dian.colombia/IWcfDianCustomerServices/GetStatusZip";

    public PackageData prepareSecuredTestSet(DianElectronicDocument document, DianConfiguration configuration) {
        if (configuration == null || configuration.getEnvironment() != DianEnvironment.HABILITACION) {
            throw new IllegalArgumentException("Se requiere la configuración DIAN de habilitación");
        }
        if (document == null || document.getEmpresa() == null
                || !document.getEmpresa().getNit().equals(configuration.getEmpresa().getNit())) {
            throw new IllegalArgumentException("El documento y la configuración pertenecen a empresas diferentes");
        }
        String testSetId = required(configuration.getTestSetId(), "TestSetId");
        PackageData unsigned = prepare(document, testSetId);
        DianCertificateService.SigningMaterial material = certificates.load(configuration);
        String serviceUrl = configuration.getDianServiceUrl() == null
                || configuration.getDianServiceUrl().isBlank()
                ? TEST_SERVICE_URL : configuration.getDianServiceUrl().trim();
        byte[] securedEnvelope = wsSecurity.sign(unsigned.soapEnvelope(), material.pkcs12(), material.password(),
                serviceUrl, SEND_TEST_SET_ACTION);
        return new PackageData(unsigned.xmlFileName(), unsigned.zipFileName(), unsigned.zip(), securedEnvelope);
    }

    public byte[] prepareSecuredStatusQuery(DianConfiguration configuration, String trackId) {
        if (configuration == null) throw new IllegalArgumentException("Falta la configuración DIAN");
        DianCertificateService.SigningMaterial material = certificates.load(configuration);
        String serviceUrl = serviceUrl(configuration);
        return wsSecurity.sign(soap.getStatusZip(required(trackId, "TrackId")), material.pkcs12(),
                material.password(), serviceUrl, GET_STATUS_ZIP_ACTION);
    }

    public String serviceUrl(DianConfiguration configuration) {
        if (configuration == null) throw new IllegalArgumentException("Falta la configuración DIAN");
        if (configuration.getDianServiceUrl() != null && !configuration.getDianServiceUrl().isBlank()) {
            return configuration.getDianServiceUrl().trim();
        }
        return configuration.getEnvironment() == DianEnvironment.PRODUCCION
                ? PRODUCTION_SERVICE_URL : TEST_SERVICE_URL;
    }

    private PackageData prepare(DianElectronicDocument document, String testSetId) {
        if (document == null || document.getEmpresa() == null || document.getVenta() == null) {
            throw new IllegalArgumentException("El documento DIAN está incompleto");
        }
        if (document.getStatus() != proyecto.dian.model.DianDocumentStatus.SIGNED) {
            throw new IllegalStateException("La factura debe estar firmada antes de empaquetarla");
        }
        byte[] signedXml = storage.readXml(document.getEmpresa().getNit(), document.getId(), "signed");
        DianFileNameService.Names fileNames = names.invoice(document.getEmpresa().getNit(),
                document.getVenta().getFecha().toLocalDate(), document.getConsecutive());
        byte[] zipped = zip.zipXml(fileNames.xml(), signedXml);
        byte[] envelope = soap.sendTestSetAsync(fileNames.zip(), zipped, testSetId);
        return new PackageData(fileNames.xml(), fileNames.zip(), zipped, envelope);
    }

    private String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Falta " + name);
        return value.trim();
    }

    public record PackageData(String xmlFileName, String zipFileName, byte[] zip, byte[] soapEnvelope) {}
}
