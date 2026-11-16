package proyecto.dian.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class DianHttpSoapTransport {
    private static final int MAX_RESPONSE_BYTES = 5 * 1024 * 1024;
    private final boolean enabled;
    private final HttpClient client;
    private final Duration requestTimeout;

    public DianHttpSoapTransport(@Value("${dian.transmission-enabled:false}") boolean enabled,
                                 @Value("${dian.http.connect-timeout-seconds:10}") int connectTimeoutSeconds,
                                 @Value("${dian.http.request-timeout-seconds:30}") int requestTimeoutSeconds) {
        this.enabled = enabled;
        this.requestTimeout = Duration.ofSeconds(between(requestTimeoutSeconds, 1, 120, "request timeout"));
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(between(connectTimeoutSeconds, 1, 60, "connect timeout")))
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    public Response exchange(String serviceUrl, String soapAction, byte[] envelope) {
        if (!enabled) {
            throw new IllegalStateException("La transmisión DIAN está desactivada por seguridad");
        }
        if (serviceUrl == null || serviceUrl.isBlank() || soapAction == null || soapAction.isBlank()) {
            throw new IllegalArgumentException("URL y acción SOAP son obligatorias");
        }
        if (envelope == null || envelope.length == 0) throw new IllegalArgumentException("El SOAP está vacío");
        URI uri = URI.create(serviceUrl);
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("La URL DIAN debe usar HTTPS");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(requestTimeout)
                    .header("Content-Type", "application/soap+xml; charset=utf-8; action=\"" + soapAction + "\"")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(envelope)).build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.body().length > MAX_RESPONSE_BYTES) {
                throw new IllegalStateException("La respuesta DIAN supera el límite permitido");
            }
            return new Response(response.statusCode(), response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La comunicación con la DIAN fue interrumpida", exception);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("No fue posible conectar con el servicio DIAN", exception);
        }
    }

    private int between(int value, int min, int max, String name) {
        if (value < min || value > max) throw new IllegalArgumentException("Valor inválido para " + name);
        return value;
    }

    public record Response(int httpStatus, byte[] body) {}
}
