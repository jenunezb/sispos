package proyecto.entidades;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @ToString
@Entity
public class Inventario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Stock actual disponible en la sede
    @Column(nullable = false)
    private Integer stockActual = 0;

    // Total histórico de entradas
    @Column(nullable = false)
    private Integer entradas = 0;

    // Total histórico de salidas (ventas)
    @Column(nullable = false)
    private Integer salidas = 0;

    // Total histórico de pérdidas (daños, vencidos, robos)
    @Column(nullable = false)
    private Integer perdidas = 0;

    // Relación con producto
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    // Relación con sede
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;
}
