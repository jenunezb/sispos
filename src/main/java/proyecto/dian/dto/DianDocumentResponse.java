package proyecto.dian.dto;

import proyecto.dian.model.*;

public record DianDocumentResponse(
        Long id,
        Long saleId,
        DianDocumentType documentType,
        DianEnvironment environment,
        String fullNumber,
        String cufeOrCude,
        DianDocumentStatus status,
        String dianStatusCode,
        String dianStatusMessage
) {
    public static DianDocumentResponse from(DianElectronicDocument document) {
        return new DianDocumentResponse(document.getId(),
                document.getVenta() == null ? null : document.getVenta().getId(),
                document.getDocumentType(), document.getEnvironment(), document.getFullNumber(),
                document.getCufeOrCude(), document.getStatus(), document.getDianStatusCode(),
                document.getDianStatusMessage());
    }
}
