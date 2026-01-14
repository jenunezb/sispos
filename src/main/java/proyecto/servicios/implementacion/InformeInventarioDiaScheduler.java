package proyecto.servicios.implementacion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import proyecto.dto.InventarioDelDia;
import proyecto.entidades.InformeInventarioDia;
import proyecto.repositorios.InformeInventarioDiaRepository;
import proyecto.servicios.interfaces.InventarioServicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InformeInventarioDiaScheduler {

    private final InventarioServicio inventarioServicio; // tu servicio de inventario
    private final InformeInventarioDiaRepository informeRepository;

    @Scheduled(cron = "0 59 23 * * *", zone = "America/Bogota") // Todos los días a 23:59 hora Colombia
    public void guardarInformeDelDia() throws JsonProcessingException {

        ZoneId zonaColombia = ZoneId.of("America/Bogota");
        LocalDate fechaHoyColombia = LocalDate.now(zonaColombia);

        for (Long sedeId : obtenerTodasLasSedes()) {

            // Fechas para filtrar inventario del día
            LocalDateTime inicio = fechaHoyColombia.atStartOfDay();
            LocalDateTime fin = fechaHoyColombia.atTime(23, 59, 59, 999_999_999);

            // Obtenemos inventario del día usando tu método existente
            List<InventarioDelDia> inventarioDia = inventarioServicio.obtenerInventarioDia(sedeId, inicio, fin);

            if (inventarioDia.isEmpty()) continue; // opcional: no guardar si no hay movimientos

            InformeInventarioDia informe = new InformeInventarioDia();
            informe.setSedeId(sedeId);
            informe.setFecha(fechaHoyColombia);
            informe.setTotalVendido(
                    inventarioDia.stream().mapToDouble(i -> i.totalVendido()).sum()
            );

            // Guardamos los datos como JSON
            ObjectMapper mapper = new ObjectMapper();
            informe.setDatosJson(mapper.writeValueAsString(inventarioDia));

            informeRepository.save(informe);
        }

        System.out.println("✅ Informes del día guardados automáticamente (" + LocalDateTime.now(zonaColombia) + ")");
    }

    private List<Long> obtenerTodasLasSedes() {
        // Aquí debes traer los IDs de todas las sedes activas desde tu repositorio de Sede
        return List.of(1L, 2L, 3L); // ejemplo temporal
    }
}
