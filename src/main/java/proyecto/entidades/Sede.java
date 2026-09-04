package proyecto.entidades;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @ToString(exclude = {"administrador", "administradoresAsignados", "vendedores", "materiasPrimas", "empresa"})
@Entity
public class Sede {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ubicacion;

    @Column(name = "direccion_fiscal", length = 255)
    private String direccionFiscal;

    @Column(name = "municipio_codigo", length = 10)
    private String municipioCodigo;

    @Column(name = "departamento_codigo", length = 10)
    private String departamentoCodigo;

    @Column(name = "pais_codigo", length = 3)
    private String paisCodigo;

    @Column(name = "codigo_postal", length = 20)
    private String codigoPostal;

    @Column(name = "impresion_cocina_habilitada", nullable = false)
    private Boolean impresionCocinaHabilitada = true;

    @ManyToOne
    @JoinColumn(name = "admin_id")
    private Administrador administrador;

    @ManyToMany(mappedBy = "sedesAsignadas")
    private List<Administrador> administradoresAsignados = new ArrayList<>();

    @OneToMany(mappedBy = "sede")
    private List<Vendedor> vendedores;

    @OneToMany(mappedBy = "sede")
    private List<MateriaPrimaSede> materiasPrimas = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Column(name = "admin_pin_hash", length = 120)
    private String adminPinHash;

    @Column(name = "admin_pin_intentos_fallidos", nullable = false)
    private Integer adminPinIntentosFallidos = 0;

    @Column(name = "admin_pin_bloqueado_hasta")
    private LocalDateTime adminPinBloqueadoHasta;
}
