package proyecto.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenDTO {
    @NotNull
    private String token;
    private String estadoSuscripcion;
    private String fechaVencimientoSuscripcion;
    private String mensajeSuscripcion;
    private Long sedeId;
    private String plan;
    private Boolean gastosHabilitados;
    private Boolean cajaHabilitada;
}
