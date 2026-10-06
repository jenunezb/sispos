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
@Table(name = "gasto_diario")
public class GastoDiario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String descripcion;

    @Column(nullable = false)
    private Double valor;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_pago", nullable = false)
    private ModoPago modoPago;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @ManyToOne
    @JoinColumn(name = "administrador_id")
    private Administrador administrador;

    @ManyToOne
    @JoinColumn(name = "vendedor_id")
    private Vendedor vendedor;
}
