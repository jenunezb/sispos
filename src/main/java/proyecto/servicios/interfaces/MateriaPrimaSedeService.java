package proyecto.servicios.interfaces;

import proyecto.dto.CrearMateriaPrimaDTO;
import proyecto.dto.MateriaPrimaSedeDTO;
import proyecto.dto.ProductoMateriaPrimaRequestDTO;
import proyecto.entidades.MateriaPrima;
import proyecto.entidades.MateriaPrimaSede;

import java.util.List;

public interface MateriaPrimaSedeService {

    public void crearMateriaPrima(CrearMateriaPrimaDTO crearMateriaPrimaDTO);
    /**
     * Calcula cuántos vasos se pueden hacer con la materia prima en una sede.
     */
    int calcularVasosDisponibles(Long materiaPrimaId, Long sedeId);

    /**
     * Descuenta la materia prima de una sede al vender cierta cantidad de vasos.
     */
    void descontarPorVenta(Long materiaPrimaId, Long sedeId, int vasosVendidos);

    /**
     * Ajusta la cantidad de materia prima de una sede (entrada o corrección manual).
     */
    void ajustarCantidad(Long materiaPrimaId, Long sedeId, double ml);

    ProductoMateriaPrimaRequestDTO vincularMateriaPrima(Long productoId, Long materiaPrimaId, double mlConsumidos);

    List<MateriaPrimaSedeDTO> listarTodas();

}

