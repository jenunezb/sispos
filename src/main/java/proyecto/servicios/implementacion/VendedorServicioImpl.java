package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import proyecto.dto.BalanceSedeVendedor;
import proyecto.dto.VendedorDTO;
import proyecto.entidades.ModoPago;
import proyecto.entidades.Sede;
import proyecto.entidades.Vendedor;
import proyecto.entidades.Venta;
import proyecto.repositorios.SedeRepository;
import proyecto.repositorios.VendedorRepository;
import proyecto.repositorios.VentaRepository;
import proyecto.servicios.interfaces.VendedorServicio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VendedorServicioImpl implements VendedorServicio {

    @Autowired
    private final VendedorRepository vendedorRepository;
    private final SedeRepository sedeRepository;
    private final VentaRepository ventaRepository;

    @Override
    public void cambiarEstado(Long codigo, Boolean estado) {

        Vendedor vendedor = vendedorRepository.findById(codigo)
                .orElseThrow(() -> new RuntimeException("Vendedor no encontrado"));

        vendedor.setEstado(estado);
        vendedorRepository.save(vendedor);
    }

    @Override
    public List<VendedorDTO> listarVendedores() {

        return vendedorRepository.findAllByOrderByNombreAsc()
                .stream()
                .map(v -> new VendedorDTO(
                        v.getCodigo(),
                        v.getNombre(),
                        v.getCedula(),
                        v.getCorreo(),
                        v.getTelefono(),
                        v.getCiudad().getNombre(),
                        v.isEstado()
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
                .sedeNombre(sede1.getNombre())
                .totalVentas(totalVentas)
                .ventasEfectivo(ventasEfectivo)
                .ventasTransferencia(ventasTransferencia)
                .cantidadVentas(cantidadVentas)
                .build();
    }

}
