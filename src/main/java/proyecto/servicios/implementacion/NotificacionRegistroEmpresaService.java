package proyecto.servicios.implementacion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import proyecto.eventos.EmpresaRegistradaEvent;

@Service
@Slf4j
public class NotificacionRegistroEmpresaService {

    private final JavaMailSender javaMailSender;
    private final boolean correoHabilitado;
    private final String destinatario;
    private final String remitente;

    public NotificacionRegistroEmpresaService(
            JavaMailSender javaMailSender,
            @Value("${notificaciones.correo.habilitado:true}") boolean correoHabilitado,
            @Value("${notificaciones.registro-empresa.destinatario:administrador@steelsoft.com.co}") String destinatario,
            @Value("${spring.mail.username:}") String remitente
    ) {
        this.javaMailSender = javaMailSender;
        this.correoHabilitado = correoHabilitado;
        this.destinatario = destinatario;
        this.remitente = remitente;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void notificar(EmpresaRegistradaEvent evento) {
        if (!correoHabilitado || destinatario == null || destinatario.isBlank()) {
            return;
        }

        try {
            SimpleMailMessage correo = new SimpleMailMessage();
            correo.setTo(destinatario);
            if (remitente != null && !remitente.isBlank()) {
                correo.setFrom(remitente);
            }
            correo.setSubject("Nueva empresa registrada - " + evento.nombreEmpresa());
            correo.setText(construirMensaje(evento));
            javaMailSender.send(correo);
        } catch (Exception excepcion) {
            log.warn("No se pudo notificar el registro de la empresa {}: {}",
                    evento.nit(), excepcion.getMessage());
        }
    }

    private String construirMensaje(EmpresaRegistradaEvent evento) {
        return "Se registro una nueva empresa en Steelsoft.\n\n"
                + "Empresa: " + evento.nombreEmpresa() + "\n"
                + "NIT: " + evento.nit() + "\n"
                + "Administrador: " + evento.nombreAdministrador() + "\n"
                + "Correo: " + evento.correoAdministrador() + "\n"
                + "Celular: " + evento.celularAdministrador() + "\n"
                + "Sede principal: " + evento.sedePrincipal() + "\n"
                + "Ubicacion: " + evento.ubicacionSede() + "\n"
                + "Fin del periodo gratuito: " + evento.vencimientoPrueba() + "\n";
    }
}
