package proyecto.dian.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import proyecto.dian.dto.DianSaleValidationResponse;
import proyecto.entidades.*;
import proyecto.repositorios.VentaRepository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DianSaleValidationService {
    private final VentaRepository sales;
    private final DianTenantContextService tenantContext;

    public DianSaleValidationResponse validate(String authorization, Long saleId) {
        Empresa company = tenantContext.requireCompanyAdministrator(authorization);
        Venta sale = sales.findByIdAndSedeEmpresaNit(saleId, company.getNit())
                .orElseThrow(() -> new IllegalArgumentException("Venta no encontrada para la empresa autenticada"));
        return validate(sale);
    }

    DianSaleValidationResponse validate(Venta sale) {
        Set<String> missing = new LinkedHashSet<>();
        if (sale == null) throw new IllegalArgumentException("La venta es obligatoria");
        if (Boolean.TRUE.equals(sale.getAnulado())) missing.add("venta.anulada");
        required(sale.getFecha(), "venta.fecha", missing);
        requiredText(sale.getMonedaCodigo(), "venta.monedaCodigo", missing);
        requiredText(sale.getFormaPagoDian(), "venta.formaPagoDian", missing);
        requiredText(sale.getMedioPagoDian(), "venta.medioPagoDian", missing);

        Sede branch = sale.getSede();
        if (branch == null) {
            missing.add("venta.sede");
        } else {
            Empresa company = branch.getEmpresa();
            if (company == null) {
                missing.add("sede.empresa");
            } else {
                required(company.getNit(), "empresa.nit", missing);
                requiredText(company.getDv(), "empresa.dv", missing);
                requiredText(first(company.getRazonSocial(), company.getNombre()), "empresa.razonSocial", missing);
                requiredText(company.getTipoDocumentoFiscal(), "empresa.tipoDocumentoFiscal", missing);
                requiredText(company.getTipoPersonaFiscal(), "empresa.tipoPersonaFiscal", missing);
                requiredText(company.getResponsabilidadFiscal(), "empresa.responsabilidadFiscal", missing);
                requiredText(company.getRegimenFiscal(), "empresa.regimenFiscal", missing);
                requiredText(company.getCorreoFacturacion(), "empresa.correoFacturacion", missing);
                requiredText(first(branch.getDireccionFiscal(), company.getDireccionFiscal()), "emisor.direccionFiscal", missing);
                requiredText(first(branch.getMunicipioCodigo(), company.getMunicipioCodigo()), "emisor.municipioCodigo", missing);
                requiredText(first(branch.getMunicipioNombre(), company.getMunicipioNombre()), "emisor.municipioNombre", missing);
                requiredText(first(branch.getDepartamentoCodigo(), company.getDepartamentoCodigo()), "emisor.departamentoCodigo", missing);
                requiredText(first(branch.getDepartamentoNombre(), company.getDepartamentoNombre()), "emisor.departamentoNombre", missing);
                requiredText(first(branch.getPaisCodigo(), company.getPaisCodigo()), "emisor.paisCodigo", missing);
                requiredText(first(branch.getPaisNombre(), company.getPaisNombre()), "emisor.paisNombre", missing);
                requiredText(first(branch.getCodigoPostal(), company.getCodigoPostal()), "emisor.codigoPostal", missing);
            }
        }

        Cliente customer = sale.getCliente();
        if (customer == null) {
            missing.add("venta.cliente");
        } else {
            requiredText(customer.getNombre(), "cliente.nombre", missing);
            requiredText(customer.getDocumento(), "cliente.documento", missing);
            requiredText(customer.getTipoDocumentoFiscal(), "cliente.tipoDocumentoFiscal", missing);
            requiredText(customer.getTipoPersonaFiscal(), "cliente.tipoPersonaFiscal", missing);
            requiredText(customer.getResponsabilidadFiscal(), "cliente.responsabilidadFiscal", missing);
            requiredText(customer.getRegimenFiscal(), "cliente.regimenFiscal", missing);
            requiredText(customer.getDireccion(), "cliente.direccion", missing);
            requiredText(customer.getMunicipioCodigo(), "cliente.municipioCodigo", missing);
            requiredText(customer.getMunicipioNombre(), "cliente.municipioNombre", missing);
            requiredText(customer.getDepartamentoCodigo(), "cliente.departamentoCodigo", missing);
            requiredText(customer.getDepartamentoNombre(), "cliente.departamentoNombre", missing);
            requiredText(customer.getPaisCodigo(), "cliente.paisCodigo", missing);
            requiredText(customer.getPaisNombre(), "cliente.paisNombre", missing);
            requiredText(customer.getCodigoPostal(), "cliente.codigoPostal", missing);
            requiredText(customer.getCorreo(), "cliente.correo", missing);
        }

        if (sale.getDetalles() == null || sale.getDetalles().isEmpty()) {
            missing.add("venta.detalles");
        } else {
            for (int index = 0; index < sale.getDetalles().size(); index++) {
                validateLine(sale.getDetalles().get(index), index, missing);
            }
        }
        return new DianSaleValidationResponse(sale.getId(), missing.isEmpty(), List.copyOf(missing));
    }

    private void validateLine(DetalleVenta detail, int index, Set<String> missing) {
        String path = "venta.detalles[" + index + "]";
        if (detail == null) { missing.add(path); return; }
        required(detail.getCantidad(), path + ".cantidad", missing);
        required(detail.getPrecioUnitarioFiscal(), path + ".precioUnitarioFiscal", missing);
        required(detail.getDescuentoFiscal(), path + ".descuentoFiscal", missing);
        Producto product = detail.getProducto();
        if (product == null) {
            missing.add(path + ".producto");
            return;
        }
        requiredText(product.getNombre(), path + ".producto.nombre", missing);
        requiredText(product.getCodigoEstandarFiscal(), path + ".producto.codigoEstandarFiscal", missing);
        requiredText(product.getUnidadMedidaDian(), path + ".producto.unidadMedidaDian", missing);
        required(product.getPrecioIncluyeImpuestos(), path + ".producto.precioIncluyeImpuestos", missing);
        if (product.getTarifaIva() == null && product.getTarifaInc() == null && product.getTarifaIca() == null) {
            missing.add(path + ".producto.tarifasImpuesto");
        }
    }

    private void required(Object value, String field, Set<String> missing) {
        if (value == null) missing.add(field);
    }

    private void requiredText(String value, String field, Set<String> missing) {
        if (value == null || value.isBlank()) missing.add(field);
    }

    private String first(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred : fallback;
    }
}
