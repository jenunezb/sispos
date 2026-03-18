package proyecto.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "suscripcion_sede")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class SuscripcionSede {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "sede_id", nullable = false, unique = true)
    private Sede sede;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cobro", nullable = false)
    private TipoCobroSuscripcion tipoCobro = TipoCobroSuscripcion.MENSUAL;

    @Column(name = "precio_mensual", nullable = false)
    private Double precioMensual = 0D;

    @Column(name = "precio_anual", nullable = false)
    private Double precioAnual = 0D;

    @Column(name = "fecha_inicio_servicio")
    private LocalDate fechaInicioServicio;

    @Column(name = "fecha_ultimo_pago")
    private LocalDate fechaUltimoPago;

    @Column(name = "fecha_proximo_vencimiento")
    private LocalDate fechaProximoVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_servicio", nullable = false)
    private EstadoSuscripcionSede estadoServicio = EstadoSuscripcionSede.VENCIDO;

    @Column(length = 500)
    private String observacion;

    @Column(nullable = false)
    private Boolean activa = true;

    @OneToMany(mappedBy = "suscripcion", cascade = CascadeType.ALL)
    private List<PagoSuscripcionSede> pagos = new ArrayList<>();
}
