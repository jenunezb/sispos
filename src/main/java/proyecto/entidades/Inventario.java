package proyecto.entidades;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @ToString
@Entity
public class Inventario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer stockActual = 0;

    @Column(nullable = false)
    private Integer entradas = 0;

    @Column(nullable = false)
    private Integer salidas = 0;

    @Column(nullable = false)
    private Integer perdidas = 0;

    @Column(nullable = false)
    private Integer stockMinimo = 0;

    @Column(nullable = false)
    private Boolean alertaStockMinimoActiva = false;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;
}

