package proyecto.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @ToString
@Entity
public class Producto implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @PositiveOrZero
    private Double precioProduccion;

    @Column(nullable = false)
    private Double precioVenta;

    @Column(length = 50)
    private String categoria;

    @Column(nullable = false)
    private Boolean estado = true;

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductoMateriaPrima> materiasPrimas = new ArrayList<>();

}