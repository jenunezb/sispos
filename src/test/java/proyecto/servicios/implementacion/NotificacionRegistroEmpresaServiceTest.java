package proyecto.servicios.implementacion;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import proyecto.eventos.EmpresaRegistradaEvent;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificacionRegistroEmpresaServiceTest {

    @Test
    void debeNotificarElRegistroSinIncluirLaContrasena() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        NotificacionRegistroEmpresaService servicio = new NotificacionRegistroEmpresaService(
                mailSender,
                true,
                "administrador@steelsoft.com.co",
                "notificaciones@steelsoft.com.co"
        );

        servicio.notificar(new EmpresaRegistradaEvent(
                900123456L,
                "Empresa Demo",
                "Ana Principal",
                "ana@empresa.com",
                3001234567L,
                "Sede Principal",
                "Centro",
                LocalDate.of(2026, 10, 29)
        ));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage enviado = captor.getValue();
        assertEquals("administrador@steelsoft.com.co", enviado.getTo()[0]);
        assertEquals("Nueva empresa registrada - Empresa Demo", enviado.getSubject());
        assertEquals("notificaciones@steelsoft.com.co", enviado.getFrom());
        assertFalse(enviado.getText().toLowerCase().contains("password"));
        assertFalse(enviado.getText().toLowerCase().contains("contrasena"));
    }
}
