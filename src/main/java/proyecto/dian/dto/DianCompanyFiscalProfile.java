package proyecto.dian.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import proyecto.entidades.Empresa;

public record DianCompanyFiscalProfile(
        @Size(max = 10) String tipoDocumentoFiscal,
        @Size(max = 10) String tipoPersonaFiscal,
        @Size(max = 255) String razonSocial,
        @Size(max = 255) String nombreComercial,
        @Size(max = 100) String responsabilidadFiscal,
        @Size(max = 100) String regimenFiscal,
        @Size(max = 10) String tributoCodigo,
        @Size(max = 100) String tributoNombre,
        @Size(max = 255) String direccionFiscal,
        @Size(max = 10) String municipioCodigo,
        @Size(max = 100) String municipioNombre,
        @Size(max = 10) String departamentoCodigo,
        @Size(max = 100) String departamentoNombre,
        @Size(max = 3) String paisCodigo,
        @Size(max = 100) String paisNombre,
        @Size(max = 20) String codigoPostal,
        @Email @Size(max = 254) String correoFacturacion,
        @Size(max = 30) String telefonoFacturacion
) {
    public static DianCompanyFiscalProfile from(Empresa company) {
        return new DianCompanyFiscalProfile(company.getTipoDocumentoFiscal(), company.getTipoPersonaFiscal(),
                company.getRazonSocial(), company.getNombreComercial(), company.getResponsabilidadFiscal(),
                company.getRegimenFiscal(), company.getTributoCodigo(), company.getTributoNombre(),
                company.getDireccionFiscal(), company.getMunicipioCodigo(), company.getMunicipioNombre(),
                company.getDepartamentoCodigo(), company.getDepartamentoNombre(), company.getPaisCodigo(),
                company.getPaisNombre(), company.getCodigoPostal(), company.getCorreoFacturacion(),
                company.getTelefonoFacturacion());
    }
}
