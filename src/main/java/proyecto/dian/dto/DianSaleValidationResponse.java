package proyecto.dian.dto;

import java.util.List;

public record DianSaleValidationResponse(Long saleId, boolean ready, List<String> missingFields) {}
