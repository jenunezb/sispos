package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import proyecto.dto.CiudadGetDTO;
import proyecto.dto.LoginDTO;
import proyecto.dto.TokenDTO;
import proyecto.entidades.Cuenta;
import proyecto.entidades.Vendedor;
import proyecto.excepciones.CorreoNoEncontradoException;
import proyecto.repositorios.CuentaRepo;
import proyecto.repositorios.VendedorRepository;
import proyecto.servicios.interfaces.AutenticacionServicio;
import proyecto.utils.JWTUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AutenticacionServicioImpl implements AutenticacionServicio {

    private final CuentaRepo cuentaRepo;
    private final JWTUtils jwtUtils;
    private final VendedorRepository vendedorRepository;


    @Override
    public TokenDTO login(LoginDTO loginDTO) throws Exception{

        Vendedor vendedor = vendedorRepository.findByCorreo(loginDTO.email())
                .orElseThrow(() -> new RuntimeException("Credenciales incorrectas"));

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        System.out.println("➡️ Correo recibido: #" + loginDTO.email() + "#");
        Optional<Cuenta> cuentaOptional = cuentaRepo.findByCorreo(loginDTO.email());
        if(cuentaOptional.isEmpty()){
            throw new CorreoNoEncontradoException("No existe el correo ingresado");
        }
        Cuenta cuenta = cuentaOptional.get();
        if( !passwordEncoder.matches(loginDTO.password(), cuenta.getPassword()) ){
            throw new Exception("La contraseña ingresada es incorrecta");
        }
        if (!Boolean.TRUE.equals(vendedor.isEstado())) {
            throw new RuntimeException(
                    "El vendedor se encuentra desactivado. Comuníquese con el administrador."
            );
        }
        return new TokenDTO( crearToken(cuenta) );
    }

    @Override
    public List<CiudadGetDTO> listarCiudades() {
        return List.of();
    }

    private String crearToken(Cuenta cuenta){
        String rol;
        String nombre;
        if( cuenta instanceof Vendedor){
            rol = "ingeniero";
            nombre = ((Vendedor) cuenta).getNombre();
        }else{
            rol = "administrador";
            nombre = "Administrador";
        }
        Map<String, Object> map = new HashMap<>();
        map.put("rol", rol);
        map.put("nombre", nombre);
        map.put("id", cuenta.getCodigo());

        return jwtUtils.generarToken(cuenta.getCorreo(), map);
    }
}
