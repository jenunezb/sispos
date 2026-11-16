package proyecto.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 30)
    private String telefono;

    @Column(length = 30)
    private String documento;

    @Column(name = "tipo_documento_fiscal", length = 10)
    private String tipoDocumentoFiscal;

    @Column(name = "dv", length = 5)
    private String dv;

    @Column(name = "tipo_persona_fiscal", length = 10)
    private String tipoPersonaFiscal;

    @Column(name = "responsabilidad_fiscal", length = 100)
    private String responsabilidadFiscal;

    @Column(name = "regimen_fiscal", length = 100)
    private String regimenFiscal;

    @Column(name = "tributo_codigo", length = 10)
    private String tributoCodigo;

    @Column(name = "tributo_nombre", length = 100)
    private String tributoNombre;

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

    @Column(length = 254)
    private String correo;

    @Column(length = 255)
    private String direccion;

    @Column(nullable = false)
    private Boolean activo = true;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;
}
