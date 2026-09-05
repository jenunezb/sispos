package proyecto.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Empresa implements Serializable {

    @Id
    private Long nit;

    @Column(length = 5)
    private String dv;

    private String nombre;

    @Column(name = "tipo_documento_fiscal", length = 10)
    private String tipoDocumentoFiscal;

    @Column(name = "tipo_persona_fiscal", length = 10)
    private String tipoPersonaFiscal;

    @Column(name = "razon_social", length = 255)
    private String razonSocial;

    @Column(name = "nombre_comercial", length = 255)
    private String nombreComercial;

    @Column(name = "responsabilidad_fiscal", length = 100)
    private String responsabilidadFiscal;

    @Column(name = "regimen_fiscal", length = 100)
    private String regimenFiscal;

    @Column(name = "tributo_codigo", length = 10)
    private String tributoCodigo;

    @Column(name = "tributo_nombre", length = 100)
    private String tributoNombre;

    @Column(name = "direccion_fiscal", length = 255)
    private String direccionFiscal;

    @Column(name = "municipio_codigo", length = 10)
    private String municipioCodigo;

    @Column(name = "municipio_nombre", length = 100)
    private String municipioNombre;

    @Column(name = "departamento_codigo", length = 10)
    private String departamentoCodigo;

    @Column(name = "departamento_nombre", length = 100)
    private String departamentoNombre;

    @Column(name = "pais_codigo", length = 3)
    private String paisCodigo;

    @Column(name = "pais_nombre", length = 100)
    private String paisNombre;

    @Column(name = "codigo_postal", length = 20)
    private String codigoPostal;

    @Column(name = "correo_facturacion", length = 254)
    private String correoFacturacion;

    @Column(name = "telefono_facturacion", length = 30)
    private String telefonoFacturacion;

    @OneToOne
    private Imagen logo;

    @Column(nullable = false)
    private Boolean impresionCocinaHabilitada = true;

    @OneToMany(mappedBy = "empresa", cascade = CascadeType.ALL)
    private List<Sede> sedes;

    @OneToMany(mappedBy = "empresa", cascade = CascadeType.ALL)
    private List<Administrador> administradores;

}
