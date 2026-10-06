package proyecto.servicios.implementacion;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import proyecto.entidades.Imagen;
import proyecto.repositorios.ImagenRepository;
import proyecto.servicios.interfaces.ImagenesServicio;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImagenesServicioImpl implements ImagenesServicio {

    private final Cloudinary cloudinary;
    private final ImagenRepository imagenRepository;

    @Override
    public Map<String, Object> subirImagen(MultipartFile imagen) throws Exception {

        if (imagen.isEmpty()) {
            throw new Exception("La imagen está vacía");
        }

        // Opcional: limitar tamaño (5MB ejemplo)
        if (imagen.getSize() > 5_000_000) {
            throw new Exception("La imagen supera el tamaño permitido (5MB)");
        }

        // Subir a Cloudinary (con carpeta)
        Map<String, Object> opciones = new HashMap<>();
        opciones.put("folder", "proyecto");

        Map resultado = cloudinary.uploader()
                .upload(imagen.getBytes(), opciones);

        String url = resultado.get("secure_url").toString();
        String publicId = resultado.get("public_id").toString();

        // Guardar en base de datos
        Imagen nuevaImagen = new Imagen();
        nuevaImagen.setUrl(url);
        nuevaImagen.setPublicId(publicId);

        Imagen guardada = imagenRepository.save(nuevaImagen);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("id", guardada.getId());
        respuesta.put("url", guardada.getUrl());
        respuesta.put("publicId", guardada.getPublicId());

        return respuesta;
    }

    @Override
    public Map<String, Object> eliminarImagen(Long id) throws Exception {

        Imagen imagen = imagenRepository.findById(id)
                .orElseThrow(() -> new Exception("Imagen no encontrada"));

        // Eliminar de Cloudinary
        cloudinary.uploader()
                .destroy(imagen.getPublicId(), ObjectUtils.emptyMap());

        // Eliminar de base de datos
        imagenRepository.delete(imagen);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Imagen eliminada correctamente");

        return respuesta;
    }
}
