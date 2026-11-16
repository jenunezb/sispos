package proyecto.controladores;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import proyecto.dto.MensajeDTO;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username:}")
    private String from;

    @GetMapping("/")
    public String health() {
        return "OK";
    }

    @GetMapping("/api/health/smtp")
    public ResponseEntity<MensajeDTO<String>> probarSmtp(@RequestParam String to) {
        try {
            CompletableFuture.runAsync(() -> {
                SimpleMailMessage mail = new SimpleMailMessage();
                mail.setTo(to);
                mail.setSubject("Prueba SMTP SisPOS");
                mail.setText("SMTP funcionando correctamente.");
                if (from != null && !from.isBlank()) {
                    mail.setFrom(from);
                }
                javaMailSender.send(mail);
            }).get(10, TimeUnit.SECONDS);

            return ResponseEntity.ok(new MensajeDTO<>(false, "Correo de prueba enviado"));
        } catch (TimeoutException e) {
            return ResponseEntity.badRequest().body(
                    new MensajeDTO<>(true, "Error SMTP: Timeout al conectar/enviar")
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new MensajeDTO<>(true, "Error SMTP: " + e.getMessage())
            );
        }
    }
}
