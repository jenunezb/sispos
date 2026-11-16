package proyecto.entidades;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @ToString
@Entity
public class Proveedor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;
    private String contacto;
    @Column(length = 30)
    private String identificacion;
    @Column(length = 254)
    private String correo;
    @Column(length = 40)
    private String telefono;
    @Column(nullable = false)
    private boolean activo = true;
    // Nullable to preserve legacy suppliers until an administrator associates them with a company.
    @ManyToOne
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;
}
