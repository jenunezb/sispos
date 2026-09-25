package proyecto.dian.dto;

import proyecto.dian.model.DianDocumentType;
import proyecto.dian.model.DianEnvironment;
import proyecto.dian.model.DianNumberingRange;

import java.time.LocalDate;

public record DianNumberingRangeResponse(
        Long id,
        DianEnvironment environment,
        DianDocumentType documentType,
        String prefix,
        String resolutionNumber,
        Long rangeFrom,
        Long rangeTo,
        Long currentNumber,
        LocalDate validFrom,
        LocalDate validUntil,
        boolean technicalKeyConfigured,
        boolean active
) {
    public static DianNumberingRangeResponse from(DianNumberingRange range) {
        return new DianNumberingRangeResponse(range.getId(), range.getEnvironment(), range.getDocumentType(),
                range.getPrefix(), range.getResolutionNumber(), range.getRangeFrom(), range.getRangeTo(),
                range.getCurrentNumber(), range.getValidFrom(), range.getValidUntil(),
                range.getTechnicalKeyEncrypted() != null && !range.getTechnicalKeyEncrypted().isBlank(),
                Boolean.TRUE.equals(range.getActive()));
    }
}
