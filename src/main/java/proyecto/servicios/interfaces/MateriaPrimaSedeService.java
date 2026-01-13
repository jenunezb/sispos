package proyecto.servicios.interfaces;

import jakarta.validation.Valid;
import proyecto.dto.*;
import proyecto.entidades.MateriaPrima;
import proyecto.entidades.MateriaPrimaSede;

import java.util.List;

public interface MateriaPrimaSedeService {

    public void crearMateriaPrima(CrearMateriaPrimaDTO crearMateriaPrimaDTO);

    public void materiaPrimaSede(Long codigoMateriaPrima, Long cogigoSede);

    public MateriaPrimaSedeResponseDTO crearYVincular(@Valid CrearMateriaPrimaSedeDTO dto);

    /**
     * Calcula cuántos vasos se pueden hacer con la materia prima en una sede.
     */
    int calcularVasosDisponibles(Long materiaPrimaId, Long sedeId);

    /**
     * Descuenta la materia prima de una sede al vender cierta cantidad de vasos.
     */
    void descontarPorVenta(Long materiaPrimaId, Long sedeId, int vasosVendidos);

    ProductoMateriaPrimaRequestDTO vincularMateriaPrima(Long productoId, Long materiaPrimaId, double mlConsumidos);

    List<MateriaPrimaSedeDTO> listarTodas();

    void actualizarMateriaPrimaSede(Long id, MateriaPrimaSedeUpdate dto);

    void vincularProducto(VincularProductoDTO dto);

}

