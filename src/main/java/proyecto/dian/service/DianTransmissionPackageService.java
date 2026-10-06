package proyecto.dian.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import proyecto.dian.model.DianElectronicDocument;

@Service
@RequiredArgsConstructor
public class DianTransmissionPackageService {
    private final DianPrivateStorageService storage;
    private final DianZipService zip;
    private final DianFileNameService names;
    private final DianSoapMessageService soap;

    public PackageData prepareTestSet(DianElectronicDocument document, String testSetId) {
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

    public record PackageData(String xmlFileName, String zipFileName, byte[] zip, byte[] soapEnvelope) {}
}
