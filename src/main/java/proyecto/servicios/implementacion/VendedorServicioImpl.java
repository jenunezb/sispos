package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import proyecto.entidades.Vendedor;
import proyecto.repositorios.VendedorRepository;
import proyecto.servicios.interfaces.VendedorServicio;

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
}
