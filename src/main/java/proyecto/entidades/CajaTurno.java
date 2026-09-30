package proyecto.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Entity
@Table(name = "caja_turno")
public class CajaTurno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @ManyToOne
    @JoinColumn(name = "administrador_apertura_id")
    private Administrador administradorApertura;

    @ManyToOne
    @JoinColumn(name = "administrador_cierre_id")
    private Administrador administradorCierre;

    @ManyToOne
    @JoinColumn(name = "vendedor_apertura_id")
    private Vendedor vendedorApertura;

    @ManyToOne
    @JoinColumn(name = "vendedor_cierre_id")
    private Vendedor vendedorCierre;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCaja estado;

    @Column(name = "base_inicial", nullable = false)
    private Double baseInicial;

    @Column(name = "ventas_efectivo")
    private Double ventasEfectivo;

    @Column(name = "gastos_efectivo")
    private Double gastosEfectivo;

    @Column(name = "efectivo_esperado")
    private Double efectivoEsperado;

    @Column(name = "efectivo_contado")
    private Double efectivoContado;

    @Column(name = "diferencia")
    private Double diferencia;

    @Column(length = 500)
    private String observacion;

    @Column(name = "observacion_cierre", length = 500)
    private String observacionCierre;
}
