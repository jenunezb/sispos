package proyecto.servicios.implementacion;

import jakarta.transaction.Transactional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import proyecto.dto.ProductoActualizarDTO;
import proyecto.dto.ProductoCrearDTO;
import proyecto.dto.ProductoDTO;
import proyecto.entidades.Empresa;
import proyecto.entidades.Inventario;
import proyecto.entidades.Producto;
import proyecto.entidades.Sede;
import proyecto.repositorios.InventarioRepository;
import proyecto.repositorios.ProductoRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.servicios.interfaces.ProductoServicio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoServicioImpl implements ProductoServicio {

    @Autowired
    private final ProductoRepository productoRepository;
    private final InventarioRepository inventarioRepository;
    private final SedeRepository sedeRepository;

    @Override
    public ProductoDTO crearProducto(ProductoCrearDTO dto) {
        // 🔹 Buscar sede
        Sede sede = sedeRepository.findById(dto.sedeId())
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        Empresa empresa = sede.getEmpresa();
        if (empresa == null) {
            throw new RuntimeException("La sede no tiene una empresa asociada");
        }

        Producto producto = new Producto();
        producto.setNombre(dto.nombre());
        producto.setDescripcion(dto.descripcion());
        producto.setPrecioProduccion(dto.precioProduccion());
        producto.setPrecioVenta(dto.precioVenta());
        producto.setCategoria(dto.categoria());
        producto.setEstado(true);
        producto.setEmpresa(empresa);

        Producto guardado = productoRepository.save(producto);

        // 🔹 Crear inventario automáticamente
        Inventario inventario = new Inventario();
        inventario.setProducto(guardado);
        inventario.setSede(sede);
        inventario.setEntradas(0);
        inventario.setSalidas(0);
        inventario.setPerdidas(0);
        inventario.setStockActual(0);

        inventarioRepository.save(inventario);

        return mapToDTO(guardado);

    }

    @Override
    @Transactional
    public void desactivarProducto(Long codigo) {

        Producto producto = productoRepository.findById(codigo)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        producto.setActivo(false);

        productoRepository.save(producto);
    }

    @Override
    public List<ProductoDTO> listarProductos(Long empresaNit, Long sedeId) {

        List<Producto> productos = sedeId != null
                ? productoRepository.findActivosByEmpresaNitAndSedeIdOrderByCodigoAsc(empresaNit, sedeId)
                : productoRepository.findByActivoTrueAndEmpresaNitOrderByCodigoAsc(empresaNit);

        return productos
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public ProductoDTO actualizarProducto(ProductoActualizarDTO dto) {
        Producto producto = productoRepository.findById(dto.codigo())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        producto.setNombre(dto.nombre());
        producto.setDescripcion(dto.descripcion());
        producto.setPrecioProduccion(dto.precioProduccion());
        producto.setPrecioVenta(dto.precioVenta());
        producto.setCategoria(dto.categoria());
        producto.setEstado(dto.estado());

        Producto actualizado = productoRepository.save(producto);
        return mapToDTO(actualizado);
    }

    @Override
    public int importarProductosCsv(MultipartFile archivo, Long empresaNit) {
        if (archivo == null || archivo.isEmpty()) {
            throw new RuntimeException("El archivo CSV está vacío");
        }

        List<Sede> sedes = sedeRepository.findByEmpresaNit(empresaNit);
        if (sedes.isEmpty()) {
            throw new RuntimeException("La empresa no tiene sedes asociadas");
        }

        Sede sedePrincipal = sedes.get(0);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            int totalImportados = 0;

            for (CSVRecord record : parser) {
                String nombre = valor(record, "Nombre");
                if (nombre == null || nombre.isBlank()) {
                    continue;
                }

                Producto producto = new Producto();
                producto.setNombre(nombre);
                producto.setDescripcion(valor(record, "Descripcion"));
                producto.setPrecioProduccion(parseDouble(valor(record, "Precio Produccion")));
                producto.setPrecioVenta(parseDouble(valor(record, "Precio Venta")));
                producto.setEstado(parseEstado(valor(record, "Estado")));
                producto.setCategoria(null);
                producto.setEmpresa(sedePrincipal.getEmpresa());

                Producto guardado = productoRepository.save(producto);

                Inventario inventario = new Inventario();
                inventario.setProducto(guardado);
                inventario.setSede(sedePrincipal);
                inventario.setEntradas(0);
                inventario.setSalidas(0);
                inventario.setPerdidas(0);
                inventario.setStockActual(0);
                inventarioRepository.save(inventario);

                totalImportados++;
            }

            return totalImportados;
        } catch (IOException e) {
            throw new RuntimeException("No fue posible leer el archivo CSV", e);
        }
    }

    private String valor(CSVRecord record, String columna) {
        if (!record.isMapped(columna)) {
            return null;
        }

        String valor = record.get(columna);
        return valor == null ? null : valor.trim();
    }

    private Double parseDouble(String valor) {
        if (valor == null || valor.isBlank()) {
            return 0D;
        }

        return Double.parseDouble(valor.replace(",", "."));
    }

    private Boolean parseEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return true;
        }

        return estado.equalsIgnoreCase("activo") || estado.equalsIgnoreCase("true");
    }

    @Override
    public ProductoDTO obtenerProductoPorCodigo(Long codigo) {
        Producto producto = productoRepository.findById(codigo)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        return mapToDTO(producto);
    }

    private ProductoDTO mapToDTO(Producto producto) {
        return new ProductoDTO(
                producto.getCodigo(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecioProduccion(),
                producto.getPrecioVenta(),
                producto.getCategoria(),
                producto.getEstado()
        );
    }
}
