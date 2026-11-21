package proyecto.controladores;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import proyecto.dto.MensajeDTO;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {MeseroController.class, MeseroAdminController.class})
public class MeseroValidacionAdvice {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public MensajeDTO<String> validar(MethodArgumentNotValidException ex) {
        var error = ex.getBindingResult().getFieldError();
        String mensaje = error == null ? "Revisa los datos ingresados" : error.getDefaultMessage();
        return new MensajeDTO<>(true, mensaje);
    }
}
