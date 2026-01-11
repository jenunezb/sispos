package proyecto.servicios.implementacion;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import proyecto.dto.MateriaPrimaRequestDTO;
import proyecto.dto.ProductoMateriaPrimaRequestDTO;
import proyecto.entidades.*;
import proyecto.repositorios.*;
import proyecto.servicios.interfaces.MateriaPrimaSedeService;

@Service
@RequiredArgsConstructor
@Transactional
public class MateriaPrimaSedeServiceImpl implements MateriaPrimaSedeService {

    private final MateriaPrimaSedeRepository materiaPrimaSedeRepository;
    private final MateriaPrimaRepository materiaPrimaRepository;
    private final SedeRepository sedeRepository;
    private final ProductoRepository productoRepository;
    private final ProductoMateriaPrimaRepository productoMateriaPrimaRepository;



    public MateriaPrima crearMateriaPrima(MateriaPrimaRequestDTO dto, Long sedeId) {

        // 1️⃣ Crear materia prima
        MateriaPrima materiaPrima = new MateriaPrima();
        materiaPrima.setNombre(dto.nombre());
        materiaPrima.setActiva(dto.activa());

        materiaPrima = materiaPrimaRepository.save(materiaPrima);

        // 2️⃣ Asociar a la sede con cantidad y mlPorVaso
        Sede sede = sedeRepository.findById(sedeId)
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        MateriaPrimaSede mpSede = new MateriaPrimaSede();
        mpSede.setMateriaPrima(materiaPrima);
        mpSede.setSede(sede);
        mpSede.setCantidadActualMl(dto.cantidadInicialMl());
        mpSede.setMlPorVaso(dto.mlPorVaso());
        mpSede.setActiva(true);

        materiaPrimaSedeRepository.save(mpSede);

        return materiaPrima;
    }

    public int calcularVasosDisponibles(Long materiaPrimaId, Long sedeId) {
        MateriaPrimaSede mpSede = materiaPrimaSedeRepository
                .findByMateriaPrima_CodigoAndSede_Id(materiaPrimaId, sedeId)
                .orElseThrow(() -> new IllegalStateException(
                        "Materia prima no configurada en esta sede"
                ));
        return (int) (mpSede.getCantidadActualMl() / mpSede.getMlPorVaso());
    }

    public void descontarPorVenta(Long materiaPrimaId, Long sedeId, int vasosVendidos) {
        MateriaPrimaSede mpSede = materiaPrimaSedeRepository
                .findByMateriaPrima_CodigoAndSede_Id(materiaPrimaId, sedeId)
                .orElseThrow(() -> new IllegalStateException(
                        "Materia prima no configurada en esta sede"
                ));

        double mlADescontar = vasosVendidos * mpSede.getMlPorVaso();

        if (mlADescontar > mpSede.getCantidadActualMl()) {
            throw new IllegalStateException(
                    "No hay suficiente materia prima en la sede"
            );
        }

        mpSede.setCantidadActualMl(mpSede.getCantidadActualMl() - mlADescontar);
        materiaPrimaSedeRepository.save(mpSede);
    }

    public void ajustarCantidad(Long materiaPrimaId, Long sedeId, double ml) {
        MateriaPrimaSede mpSede = materiaPrimaSedeRepository
                .findByMateriaPrima_CodigoAndSede_Id(materiaPrimaId, sedeId)
                .orElseThrow(() -> new IllegalStateException(
                        "Materia prima no configurada en esta sede"
                ));

        double nuevaCantidad = mpSede.getCantidadActualMl() + ml;
        if (nuevaCantidad < 0) {
            throw new IllegalStateException("La cantidad no puede ser negativa");
        }

        mpSede.setCantidadActualMl(nuevaCantidad);
        materiaPrimaSedeRepository.save(mpSede);
    }

    public ProductoMateriaPrimaRequestDTO vincularMateriaPrima(Long productoId, Long materiaPrimaId, double mlConsumidos) {

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        MateriaPrima materiaPrima = materiaPrimaRepository.findById(materiaPrimaId)
                .orElseThrow(() -> new RuntimeException("Materia prima no encontrada"));

        // Crear la relación
        ProductoMateriaPrima pmp = new ProductoMateriaPrima();
        pmp.setProducto(producto);
        pmp.setMateriaPrima(materiaPrima);
        pmp.setMlConsumidos(mlConsumidos);

        productoMateriaPrimaRepository.save(pmp);

        return new ProductoMateriaPrimaRequestDTO(
                producto.getCodigo(),
                producto.getNombre(),
                materiaPrima.getCodigo(),
                materiaPrima.getNombre(),
                mlConsumidos
        );
    }
}


