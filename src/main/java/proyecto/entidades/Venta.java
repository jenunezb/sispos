package proyecto.entidades;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @ToString
@Entity
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime fecha;
    private Double total;

    // 🔹 Puede vender un vendedor
    @ManyToOne
    @JoinColumn(name = "vendedor_id", nullable = true)
    private Vendedor vendedor;

    // 🔹 O puede vender un administrador
    @ManyToOne
    @JoinColumn(name = "administrador_id", nullable = true)
    private Administrador administrador;

    @ManyToOne
    @JoinColumn(name = "sede_id")
    private Sede sede;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL)
    private List<DetalleVenta> detalles;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_pago", nullable = false)
    private ModoPago modoPago;

    @Column(nullable = false)
    private Boolean anulado = false;
}
