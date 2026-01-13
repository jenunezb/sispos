package proyecto.servicios.implementacion;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import proyecto.dto.*;
import proyecto.entidades.*;
import proyecto.repositorios.*;
import proyecto.servicios.interfaces.MateriaPrimaSedeService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MateriaPrimaSedeServiceImpl implements MateriaPrimaSedeService {

    private final MateriaPrimaSedeRepository materiaPrimaSedeRepository;
    private final MateriaPrimaRepository materiaPrimaRepository;
    private final ProductoRepository productoRepository;
    private final ProductoMateriaPrimaRepository productoMateriaPrimaRepository;
    private final SedeRepository sedeRepository;


    @Override
    public void crearMateriaPrima(CrearMateriaPrimaDTO crearMateriaPrimaDTO) {

        if (materiaPrimaRepository.existsByNombreIgnoreCase(crearMateriaPrimaDTO.nombre())) {
            throw new IllegalArgumentException("La materia prima ya existe");
        }

        // 1️⃣ Crear materia prima
        MateriaPrima materiaPrima = new MateriaPrima();
        materiaPrima.setNombre(crearMateriaPrimaDTO.nombre());
        materiaPrima.setActiva(crearMateriaPrimaDTO.activa());

        materiaPrimaRepository.save(materiaPrima);

    }
    @Override
    @Transactional
    public void materiaPrimaSede(Long codigoMateriaPrima, Long codigoSede) {

        MateriaPrima materiaPrima = materiaPrimaRepository.findById(codigoMateriaPrima)
                .orElseThrow(() -> new RuntimeException("Materia prima no encontrada"));

        Sede sede = sedeRepository.findById(codigoSede)
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        // 🔒 Evitar duplicados
        if (materiaPrimaSedeRepository
                .existsByMateriaPrimaAndSede(materiaPrima, sede)) {
            throw new RuntimeException("La materia prima ya está asociada a esta sede");
        }

        // 🧩 Crear relación
        MateriaPrimaSede mps = new MateriaPrimaSede();
        mps.setMateriaPrima(materiaPrima);
        mps.setSede(sede);
        mps.setCantidadActualMl(0); // inicial

        materiaPrimaSedeRepository.save(mps);
    }

    @Transactional
    public MateriaPrimaSedeResponseDTO crearYVincular(@Valid CrearMateriaPrimaSedeDTO dto) {

        MateriaPrima materiaPrima = materiaPrimaRepository
                .findByNombreIgnoreCase(dto.nombre())
                .orElseGet(() -> {
                    MateriaPrima mp = new MateriaPrima();
                    mp.setNombre(dto.nombre());
                    mp.setActiva(dto.activa());
                    return materiaPrimaRepository.save(mp);
                });

        if (materiaPrimaSedeRepository.existsByMateriaPrimaAndSedeId(
                materiaPrima, dto.sedeId())) {
            throw new RuntimeException("La materia prima ya está vinculada a esta sede");
        }

        Sede sede = sedeRepository.findById(dto.sedeId())
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        MateriaPrimaSede mpSede = new MateriaPrimaSede();
        mpSede.setMateriaPrima(materiaPrima);
        mpSede.setSede(sede);
        mpSede.setCantidadActualMl(dto.cantidadInicialMl());
        mpSede.setMlPorVaso(dto.mlPorVaso());
        mpSede.setActiva(true);

        materiaPrimaSedeRepository.save(mpSede);

        return new MateriaPrimaSedeResponseDTO(
                materiaPrima.getCodigo(),
                materiaPrima.getNombre(),
                sede.getId(),
                "Materia prima creada y vinculada correctamente"
        );
    }


    @Override
    public int calcularVasosDisponibles(Long materiaPrimaId, Long sedeId) {
        MateriaPrimaSede mpSede = materiaPrimaSedeRepository
                .findByMateriaPrima_CodigoAndSede_Id(materiaPrimaId, sedeId)
                .orElseThrow(() -> new IllegalStateException(
                        "Materia prima no configurada en esta sede"
                ));
        return (int) (mpSede.getCantidadActualMl() / mpSede.getMlPorVaso());
    }
    @Override
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
    @Override
    public ProductoMateriaPrimaRequestDTO vincularMateriaPrima(Long productoId, Long materiaPrimaId, double mlConsumidos) {

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        MateriaPrima materiaPrima = materiaPrimaRepository.findById(materiaPrimaId)
                .orElseThrow(() -> new RuntimeException("Materia prima no encontrada"));

        // 🔒 Verificar duplicado antes de guardar
        boolean existe = productoMateriaPrimaRepository.existsByProductoAndMateriaPrima(producto, materiaPrima);
        if (existe) {
            throw new RuntimeException("La materia prima ya está vinculada a este producto");
        }

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
    @Override
    public List<MateriaPrimaSedeDTO> listarTodas() {
        return materiaPrimaSedeRepository.findAll()
                .stream()
                .map(mp -> new MateriaPrimaSedeDTO(
                        mp.getId(),
                        mp.getMateriaPrima().getCodigo(),
                        mp.getMateriaPrima().getNombre(),
                        mp.getMateriaPrima().isActiva(),

                        mp.getSede().getId(),
                        mp.getSede().getNombre(),

                        mp.getCantidadActualMl(),
                        mp.getMlPorVaso(),
                        mp.isActiva()
                ))
                .toList();
    }

    /**
     * Actualizar cantidad, ml por vaso y estado activo/inactivo
     */
    public void actualizarMateriaPrimaSede(Long id, MateriaPrimaSedeUpdate dto) {
        MateriaPrimaSede mpSede = materiaPrimaSedeRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "Materia prima en sede no encontrada"
                ));

        mpSede.setCantidadActualMl(dto.cantidad());
        mpSede.setMlPorVaso(dto.mlPorVaso());
        mpSede.setActiva(dto.activa());

        materiaPrimaSedeRepository.save(mpSede);
    }

    /**
     * Vincular un producto a una materia prima en una sede
     */
    public void vincularProducto(VincularProductoDTO dto) {
        // Validar que no exista ya
        boolean existe = productoMateriaPrimaRepository.existsByMateriaPrimaIdAndProductoId(
                dto.materiaPrimaSedeId(), dto.productoId()
        );

        if (existe) {
            throw new IllegalStateException("El producto ya está vinculado a esta materia prima");
        }

        // Traer las entidades
        Producto producto = productoRepository.findById(dto.productoId())
                .orElseThrow(() -> new IllegalStateException("Producto no encontrado"));

        MateriaPrima materiaPrima = materiaPrimaRepository.findById(dto.materiaPrimaSedeId())
                .orElseThrow(() -> new IllegalStateException("Materia prima no encontrada"));

        // Crear nueva relación
        ProductoMateriaPrima nueva = new ProductoMateriaPrima();
        nueva.setProducto(producto);
        nueva.setMateriaPrima(materiaPrima);
        nueva.setMlConsumidos(dto.mlConsumidos());

        productoMateriaPrimaRepository.save(nueva);
    }


}


