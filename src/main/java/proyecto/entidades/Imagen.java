package proyecto.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Getter
@Setter
public class Imagen implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String url;

    private String publicId; // importante para borrar en Cloudinary

    @Enumerated(EnumType.STRING)
    private TipoImagen tipo;

}

