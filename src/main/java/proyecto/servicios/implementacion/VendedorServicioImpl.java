package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import proyecto.dto.VendedorDTO;
import proyecto.entidades.Vendedor;
import proyecto.repositorios.VendedorRepository;
import proyecto.servicios.interfaces.VendedorServicio;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VendedorServicioImpl implements VendedorServicio {

    @Autowired
    private VendedorRepository vendedorRepository;

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
}
