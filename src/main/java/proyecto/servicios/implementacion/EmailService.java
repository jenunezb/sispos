package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void enviarCorreo(String para, String asunto, String contenido) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(para);
            mensaje.setSubject(asunto);
            mensaje.setText(contenido);
            mensaje.setFrom("prhoteuz@gmail.com"); // 👈 IMPORTANTE

            mailSender.send(mensaje);

            System.out.println("📧 Correo enviado correctamente");

        } catch (Exception e) {
            e.printStackTrace(); // 👈 NECESITAMOS VER ESTO
            throw new RuntimeException("Error enviando correo: " + e.getMessage());
        }
    }
}