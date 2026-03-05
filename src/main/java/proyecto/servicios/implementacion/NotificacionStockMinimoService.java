package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import proyecto.entidades.Administrador;
import proyecto.entidades.Inventario;
import proyecto.repositorios.AdministradorRepository;
import proyecto.repositorios.InventarioRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacionStockMinimoService {

    private final AdministradorRepository administradorRepository;
    private final InventarioRepository inventarioRepository;
    private final JavaMailSender javaMailSender;

    @Value("${notificaciones.whatsapp.habilitado:false}")
    private boolean whatsappHabilitado;

    @Value("${notificaciones.whatsapp.api-url:https://api.callmebot.com/whatsapp.php}")
    private String whatsappApiUrl;

    @Value("${notificaciones.whatsapp.api-key:}")
    private String whatsappApiKey;

    @Value("${notificaciones.whatsapp.prefijo-pais:57}")
    private String prefijoPais;

    private final RestTemplate restTemplate = crearRestTemplateConTimeout();

    public void evaluarYNotificar(Inventario inventario, int stockActual) {
        if (inventario == null) {
            return;
        }

        int stockMinimo = inventario.getStockMinimo() == null ? 0 : inventario.getStockMinimo();

        if (stockMinimo <= 0) {
            limpiarAlertaSiAplica(inventario);
            return;
        }

        boolean estaBajoMinimo = stockActual <= stockMinimo;
        boolean alertaActiva = Boolean.TRUE.equals(inventario.getAlertaStockMinimoActiva());

        if (!estaBajoMinimo) {
            if (alertaActiva) {
                inventario.setAlertaStockMinimoActiva(false);
                inventarioRepository.save(inventario);
            }
            return;
        }

        if (alertaActiva) {
            return;
        }

        if (inventario.getSede() == null || inventario.getSede().getEmpresa() == null) {
            return;
        }

        List<Administrador> administradores = administradorRepository
                .findByEmpresaNit(inventario.getSede().getEmpresa().getNit());

        String mensaje = construirMensaje(inventario, stockActual, stockMinimo);

        for (Administrador admin : administradores) {
            enviarCorreo(admin, inventario, mensaje);
            enviarWhatsapp(admin, mensaje);
        }

        inventario.setAlertaStockMinimoActiva(true);
        inventarioRepository.save(inventario);
    }

    private void limpiarAlertaSiAplica(Inventario inventario) {
        if (Boolean.TRUE.equals(inventario.getAlertaStockMinimoActiva())) {
            inventario.setAlertaStockMinimoActiva(false);
            inventarioRepository.save(inventario);
        }
    }

    private String construirMensaje(Inventario inventario, int stockActual, int stockMinimo) {
        return "Alerta de stock minimo: el producto " + inventario.getProducto().getNombre()
                + " en la sede " + inventario.getSede().getUbicacion()
                + " llego al umbral minimo."
                + " Stock actual: " + stockActual
                + " unidades. Stock minimo configurado: " + stockMinimo + " unidades.";
    }

    private void enviarCorreo(Administrador admin, Inventario inventario, String mensaje) {
        if (admin.getCorreo() == null || admin.getCorreo().isBlank()) {
            return;
        }

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(admin.getCorreo());
            mail.setSubject("Alerta de stock minimo - " + inventario.getProducto().getNombre());
            mail.setText(mensaje);
            javaMailSender.send(mail);
        } catch (Exception e) {
            log.warn("No se pudo enviar correo de stock minimo a {}: {}", admin.getCorreo(), e.getMessage());
        }
    }

    private void enviarWhatsapp(Administrador admin, String mensaje) {
        if (!whatsappHabilitado || admin.getCelular() == null) {
            return;
        }

        try {
            String telefono = normalizarTelefono(admin.getCelular().toString());

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(whatsappApiUrl)
                    .queryParam("phone", telefono)
                    .queryParam("text", mensaje);

            if (whatsappApiKey != null && !whatsappApiKey.isBlank()) {
                builder.queryParam("apikey", whatsappApiKey);
            }

            restTemplate.getForEntity(builder.toUriString(), String.class);
        } catch (RestClientException e) {
            log.warn("No se pudo enviar WhatsApp de stock minimo a {}: {}", admin.getCelular(), e.getMessage());
        }
    }

    private String normalizarTelefono(String telefono) {
        String soloDigitos = telefono.replaceAll("\\D", "");

        if (soloDigitos.startsWith(prefijoPais)) {
            return soloDigitos;
        }

        return prefijoPais + soloDigitos;
    }

    private RestTemplate crearRestTemplateConTimeout() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);
        return new RestTemplate(factory);
    }
}
