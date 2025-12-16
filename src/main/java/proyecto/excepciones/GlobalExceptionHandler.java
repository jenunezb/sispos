package proyecto.excepciones;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import proyecto.dto.MensajeDTO;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> manejarExcepcionesGenerales(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<MensajeDTO> manejarRuntime(RuntimeException ex) {
        return ResponseEntity
                .badRequest()
                .body(new MensajeDTO(true, ex.getMessage()));
    }
}