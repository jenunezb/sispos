package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import proyecto.dto.BalanceSedeVendedor;
import proyecto.dto.VendedorDTO;
import proyecto.entidades.ModoPago;
import proyecto.entidades.Sede;
import proyecto.entidades.Vendedor;
import proyecto.entidades.Venta;
import proyecto.repositorios.MovimientoProduccionRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.repositorios.VendedorRepository;
import proyecto.repositorios.VentaRepository;
import proyecto.servicios.interfaces.VendedorServicio;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VendedorServicioImpl implements VendedorServicio {

    @Autowired
    private final VendedorRepository vendedorRepository;
    private final SedeRepository sedeRepository;
    private final VentaRepository ventaRepository;
    private final MovimientoProduccionRepository movimientoProduccionRepository;

    @Override
    public void cambiarEstado(Long codigo, Boolean estado) {

        Vendedor vendedor = vendedorRepository.findById(codigo)
                .orElseThrow(() -> new RuntimeException("Vendedor no encontrado"));

        vendedor.setEstado(estado);
        vendedorRepository.save(vendedor);
    }

    @Override
    public List<VendedorDTO> listarVendedores(Long empresaNit) {
        return mapVendedores(vendedorRepository.findVisiblesByEmpresaNit(empresaNit));
    }

    @Override
    public List<VendedorDTO> listarVendedores(Long empresaNit, List<Long> sedeIds) {
        return mapVendedores(vendedorRepository.findVisiblesByEmpresaNitAndSedeIdIn(empresaNit, sedeIds));
    }

    @Override
    @Transactional
    public void eliminarVendedor(Long codigo, Long empresaNit, List<Long> sedeIdsVisibles) {
        Vendedor vendedor = vendedorRepository.findVisibleByCodigoAndEmpresaNit(codigo, empresaNit)
                .orElseThrow(() -> new RuntimeException("Usuario comercial no encontrado"));

        if (sedeIdsVisibles != null && !sedeIdsVisibles.isEmpty()) {
            Long sedeId = vendedor.getSede() != null ? vendedor.getSede().getId() : null;
            if (sedeId == null || !sedeIdsVisibles.contains(sedeId)) {
                throw new RuntimeException("No tiene permisos para eliminar este usuario");
            }
        }

        if (ventaRepository.existsByVendedorCodigo(codigo) || movimientoProduccionRepository.existsByVendedorCodigo(codigo)) {
            throw new RuntimeException("No se puede eliminar el usuario porque tiene ventas o movimientos registrados");
        }

        vendedorRepository.delete(vendedor);
    }

    private List<VendedorDTO> mapVendedores(List<Vendedor> vendedores) {
        return vendedores
                .stream()
                .map(v -> new VendedorDTO(
                        v.getCodigo(),
                        v.getNombre(),
                        v.getCedula(),
                        v.getCorreo(),
                        v.getTelefono(),
                        v.getCiudad() != null ? v.getCiudad().getNombre() : "SIN CIUDAD",
                        v.isEstado(),
                        v.getTipoPerfil() != null ? v.getTipoPerfil().name() : "VENDEDOR"
                ))
                .toList();
    }

    public Vendedor obtenerVendedorPorCorreo(String correo) {
        System.out.println(correo);
        return vendedorRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException(
                        "Vendedor no encontrado con correo: " + correo
                ));
    }

    @Override
    public BalanceSedeVendedor balancePorSedeId(String email, LocalDateTime desde, LocalDateTime hasta) {

        Vendedor vendedor = vendedorRepository.findByCorreo(email)
                .orElseThrow(() -> new RuntimeException("Vendedor no encontrado"));

        // 2️⃣ Obtener sede del vendedor
        Sede sede = vendedor.getSede();
        Long sedeId = sede.getId();

        List<Venta> ventas = ventaRepository
                .findBySedeIdAndFechaBetween(sedeId, desde, hasta);

        double totalVentas = ventas.stream()
                .mapToDouble(Venta::getTotal)
                .sum();

        double ventasEfectivo = ventas.stream()
                .filter(v -> v.getModoPago() == ModoPago.EFECTIVO)
                .mapToDouble(Venta::getTotal)
                .sum();

        double ventasTransferencia = ventas.stream()
                .filter(v -> v.getModoPago() == ModoPago.TRANSFERENCIA)
                .mapToDouble(Venta::getTotal)
                .sum();

        long cantidadVentas = ventas.size();

        Sede sede1 = sedeRepository.findById(sedeId)
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        return BalanceSedeVendedor.builder()
                .sedeId(sede1.getId())
                .totalVentas(totalVentas)
                .ventasEfectivo(ventasEfectivo)
                .ventasTransferencia(ventasTransferencia)
                .cantidadVentas(cantidadVentas)
                .build();
    }

}
