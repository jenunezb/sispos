package proyecto.servicios.implementacion;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import proyecto.dto.MesaEstadoEventoDTO;
import proyecto.eventos.MesaEstadoActualizadoEvento;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class MesaEstadoEventosService {

    private static final long TIMEOUT_MILLIS = 30L * 60L * 1000L;
    private static final String PADDING_ANTI_BUFFER = " ".repeat(2048);
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> suscriptoresPorSede = new ConcurrentHashMap<>();

    public SseEmitter suscribir(Long sedeId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
        CopyOnWriteArrayList<SseEmitter> suscriptores =
                suscriptoresPorSede.computeIfAbsent(sedeId, ignored -> new CopyOnWriteArrayList<>());
        suscriptores.add(emitter);

        Runnable remover = () -> remover(sedeId, emitter);
        emitter.onCompletion(remover);
        emitter.onTimeout(remover);
        emitter.onError(error -> remover.run());

        try {
            emitter.send(SseEmitter.event()
                    .name("conectado")
                    .comment(PADDING_ANTI_BUFFER)
                    .data(Map.of("sedeId", sedeId)));
        } catch (IOException ex) {
            remover.run();
            emitter.completeWithError(ex);
        }
        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicarDespuesDeConfirmar(MesaEstadoActualizadoEvento evento) {
        MesaEstadoEventoDTO payload = new MesaEstadoEventoDTO("MESA_ACTUALIZADA", evento.sedeId(), evento.mesa());
        List<SseEmitter> suscriptores = suscriptoresPorSede.getOrDefault(
                evento.sedeId(), new CopyOnWriteArrayList<>());

        for (SseEmitter emitter : suscriptores) {
            try {
                emitter.send(SseEmitter.event()
                        .name("mesa-actualizada")
                        .id(evento.mesa().id() + ":" + evento.mesa().version())
                        .comment(PADDING_ANTI_BUFFER)
                        .data(payload));
            } catch (IOException | IllegalStateException ex) {
                remover(evento.sedeId(), emitter);
            }
        }
    }

    @Scheduled(fixedRate = 20000)
    public void mantenerConexionesActivas() {
        suscriptoresPorSede.forEach((sedeId, suscriptores) -> {
            for (SseEmitter emitter : suscriptores) {
                try {
                    emitter.send(SseEmitter.event().comment("keepalive"));
                } catch (IOException | IllegalStateException ex) {
                    remover(sedeId, emitter);
                }
            }
        });
    }

    private void remover(Long sedeId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> suscriptores = suscriptoresPorSede.get(sedeId);
        if (suscriptores == null) {
            return;
        }
        suscriptores.remove(emitter);
        if (suscriptores.isEmpty()) {
            suscriptoresPorSede.remove(sedeId, suscriptores);
        }
    }
}
