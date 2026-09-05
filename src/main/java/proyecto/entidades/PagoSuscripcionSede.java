package proyecto.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@Table(name = "pago_suscripcion_sede")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class PagoSuscripcionSede {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "suscripcion_id", nullable = false)
    private SuscripcionSede suscripcion;

    @ManyToOne
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pago", nullable = false)
    private TipoCobroSuscripcion tipoPago;

    @Column(nullable = false)
    private Double valor;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDate fechaPago;

    @Column(name = "periodo_desde", nullable = false)
    private LocalDate periodoDesde;

    @Column(name = "periodo_hasta", nullable = false)
    private LocalDate periodoHasta;

    @Column(name = "medio_pago", length = 100)
    private String medioPago;

    @Column(length = 500)
    private String observacion;

    @Column(name = "registrado_por", length = 150)
    private String registradoPor;
}
