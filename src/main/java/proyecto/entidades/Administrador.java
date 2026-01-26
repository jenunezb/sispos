package proyecto.entidades;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Entity
public class Administrador extends Cuenta implements Serializable {

    @Column(nullable = false)
    private boolean activo = false;

}
