package proyecto.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"items", "sede"})
@Entity
@Table(
        name = "mesa_estado",
        uniqueConstraints = @UniqueConstraint(name = "uk_mesa_estado_sede_mesa", columnNames = {"sede_id", "mesa_referencia_id"})
)
public class MesaEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mesa_referencia_id", nullable = false)
    private Long mesaReferenciaId;

    @Column(nullable = false)
    private Integer numero;

    @Column(length = 120)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @OneToMany(mappedBy = "mesaEstado", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MesaEstadoItem> items = new ArrayList<>();
}
