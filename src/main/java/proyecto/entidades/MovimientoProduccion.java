package proyecto.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@ToString
public class MovimientoProduccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "vendedor_id")
    private Vendedor vendedor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimientoProduccion tipo;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(length = 255)
    private String observacion;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @PrePersist
    void prePersist() {
        ZonedDateTime nowColombia = ZonedDateTime.now(ZoneId.of("America/Bogota"));
        fecha = nowColombia.toLocalDateTime();
    }
}
