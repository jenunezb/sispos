package proyecto.servicios.interfaces;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface ImagenesServicio {

    Map<String, Object> subirImagen(MultipartFile imagen) throws Exception;

    Map<String, Object> eliminarImagen(Long id) throws Exception;
}
