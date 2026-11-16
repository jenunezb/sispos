package proyecto.dian.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import proyecto.dian.dto.DianCompanyFiscalProfile;
import proyecto.entidades.Empresa;
import proyecto.repositorios.EmpresaRepository;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class DianCompanyFiscalProfileService {
    private final DianTenantContextService tenantContext;
    private final EmpresaRepository companies;

    @Transactional(readOnly = true)
    public DianCompanyFiscalProfile get(String authorization) {
        return DianCompanyFiscalProfile.from(tenantContext.requireCompanyAdministrator(authorization));
    }

    public DianCompanyFiscalProfile update(String authorization, DianCompanyFiscalProfile request) {
        Empresa company = tenantContext.requireCompanyAdministrator(authorization);
        company.setTipoDocumentoFiscal(clean(request.tipoDocumentoFiscal()));
        company.setTipoPersonaFiscal(clean(request.tipoPersonaFiscal()));
        company.setRazonSocial(clean(request.razonSocial()));
        company.setNombreComercial(clean(request.nombreComercial()));
        company.setResponsabilidadFiscal(clean(request.responsabilidadFiscal()));
        company.setRegimenFiscal(clean(request.regimenFiscal()));
        company.setTributoCodigo(clean(request.tributoCodigo()));
        company.setTributoNombre(clean(request.tributoNombre()));
        company.setDireccionFiscal(clean(request.direccionFiscal()));
        company.setMunicipioCodigo(clean(request.municipioCodigo()));
        company.setMunicipioNombre(clean(request.municipioNombre()));
        company.setDepartamentoCodigo(clean(request.departamentoCodigo()));
        company.setDepartamentoNombre(clean(request.departamentoNombre()));
        company.setPaisCodigo(upper(request.paisCodigo()));
        company.setPaisNombre(clean(request.paisNombre()));
        company.setCodigoPostal(clean(request.codigoPostal()));
        company.setCorreoFacturacion(lower(request.correoFacturacion()));
        company.setTelefonoFacturacion(clean(request.telefonoFacturacion()));
        return DianCompanyFiscalProfile.from(companies.save(company));
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private String upper(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : cleaned.toUpperCase(Locale.ROOT);
    }

    private String lower(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : cleaned.toLowerCase(Locale.ROOT);
    }
}
