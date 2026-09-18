package proyecto.controladores;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.*;
import proyecto.entidades.*;
import proyecto.repositorios.*;
import proyecto.servicios.implementacion.AdministradorAccesoService;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/complementos")
@RequiredArgsConstructor
public class ComplementoController {
 private final ProductoRepository productos;
 private final MateriaPrimaRepository materias;
 private final MateriaPrimaSedeRepository materiasPorSede;
 private final ProductoComplementoRepository complementos;
 private final EmpresaRepository empresas;
 private final AdministradorAccesoService acceso;

 @GetMapping("/producto/{productoId}")
 @Transactional(readOnly=true)
 public ConfiguracionComplementosProductoDTO obtener(@RequestHeader("Authorization") String auth,@PathVariable Long productoId) {
  Producto p=productos.findById(productoId).orElseThrow(()->new RuntimeException("Producto no encontrado"));
  acceso.validarAccesoAutenticadoAEmpresa(auth,p.getEmpresa().getNit());
  return construirConfiguracion(p);
 }
 private ConfiguracionComplementosProductoDTO construirConfiguracion(Producto p) {
  boolean modulo=p.getEmpresa()!=null && Boolean.TRUE.equals(p.getEmpresa().getComplementosHabilitados());
  List<ComplementoProductoDTO> lista=modulo && Boolean.TRUE.equals(p.getComplementosHabilitados())
   ? complementos.findByProductoCodigoAndActivoTrueOrderByNombreAsc(p.getCodigo()).stream().map(this::dto).toList()
   : List.of();
  return new ConfiguracionComplementosProductoDTO(modulo, Boolean.TRUE.equals(p.getComplementosHabilitados()), p.getComplementosGratis(), lista);
 }

 @PutMapping("/empresa/{empresaNit}")
 @Transactional
 public ResponseEntity<Void> configurarEmpresa(@RequestHeader("Authorization") String auth,@PathVariable Long empresaNit,@RequestBody java.util.Map<String,Boolean> body){
  Administrador a=acceso.obtenerAdministradorAutenticado(auth); acceso.validarAdministradorEmpresa(a);
  Long nit=acceso.resolverEmpresaNit(a,empresaNit);
  Empresa e=empresas.findById(nit).orElseThrow(()->new RuntimeException("Empresa no encontrada"));
  e.setComplementosHabilitados(Boolean.TRUE.equals(body.get("habilitado"))); empresas.save(e); return ResponseEntity.noContent().build();
 }

 @PutMapping("/empresa/producto/{productoId}")
 @Transactional
 public ResponseEntity<Void> configurarEmpresaDesdeProducto(@RequestHeader("Authorization") String auth,@PathVariable Long productoId,@RequestBody java.util.Map<String,Boolean> body){
  Administrador a=acceso.obtenerAdministradorAutenticado(auth); acceso.validarAdministradorEmpresa(a);
  Producto p=productos.findById(productoId).orElseThrow(()->new RuntimeException("Producto no encontrado"));
  Long nit=acceso.resolverEmpresaNit(a,p.getEmpresa().getNit());
  if(!nit.equals(p.getEmpresa().getNit())) throw new RuntimeException("Producto no pertenece a la empresa");
  p.getEmpresa().setComplementosHabilitados(Boolean.TRUE.equals(body.get("habilitado"))); empresas.save(p.getEmpresa()); return ResponseEntity.noContent().build();
 }

 @GetMapping("/empresa/{empresaNit}")
 public java.util.Map<String,Boolean> obtenerEmpresa(@RequestHeader("Authorization") String auth,@PathVariable Long empresaNit){
  Administrador a=acceso.obtenerAdministradorAutenticado(auth); Long nit=acceso.resolverEmpresaNit(a,empresaNit);
  Empresa e=empresas.findById(nit).orElseThrow(()->new RuntimeException("Empresa no encontrada"));
  return java.util.Map.of("habilitado",Boolean.TRUE.equals(e.getComplementosHabilitados()));
 }

 @PutMapping("/producto/{productoId}")
 @Transactional
 public ConfiguracionComplementosProductoDTO guardar(@RequestHeader("Authorization") String auth,@PathVariable Long productoId,@RequestBody ConfiguracionComplementosProductoDTO cfg){
  Administrador a=acceso.obtenerAdministradorAutenticado(auth);
  Producto p=productos.findById(productoId).orElseThrow(()->new RuntimeException("Producto no encontrado"));
  Long nit=acceso.resolverEmpresaNit(a,p.getEmpresa().getNit());
  if(!nit.equals(p.getEmpresa().getNit())) throw new RuntimeException("Producto no pertenece a la empresa");
  if(!Boolean.TRUE.equals(p.getEmpresa().getComplementosHabilitados()) && Boolean.TRUE.equals(cfg.habilitado())) throw new RuntimeException("Active primero el modulo de complementos de la empresa");
  p.setComplementosHabilitados(Boolean.TRUE.equals(cfg.habilitado()));
  p.setComplementosGratis(Math.max(0,cfg.gratis()==null?0:cfg.gratis())); productos.save(p);
  complementos.deleteByProductoCodigo(productoId);
  // Ejecutar el DELETE antes de recrear la lista evita conflictos con la clave
  // unica (producto, materia prima) cuando se edita una configuracion existente.
  complementos.flush();
  List<ProductoComplemento> nuevos=new ArrayList<>();
  if(cfg.complementos()!=null) for(ComplementoProductoDTO item:cfg.complementos()){
   if(item.materiaPrimaId()==null || item.cantidadConsumo()==null || item.cantidadConsumo()<=0) throw new RuntimeException("Cada topping necesita materia prima y consumo mayor a cero");
   MateriaPrima m=materias.findById(item.materiaPrimaId()).orElseThrow(()->new RuntimeException("Materia prima no encontrada"));
   boolean perteneceDirectamente = m.getEmpresa()!=null && nit.equals(m.getEmpresa().getNit());
   boolean pertenecePorSede = materiasPorSede.existsByMateriaPrimaCodigoAndSedeEmpresaNit(m.getCodigo(), nit);
   if(!perteneceDirectamente && !pertenecePorSede) throw new RuntimeException("La materia prima no pertenece a la empresa");
   // Los registros antiguos se asociaban solo a la sede. Completar la empresa
   // cuando esta vacia conserva el dato y evita que vuelva a quedar ambiguo.
   if(m.getEmpresa()==null && pertenecePorSede) {
    m.setEmpresa(p.getEmpresa());
    materias.save(m);
   }
   ProductoComplemento c=new ProductoComplemento(); c.setProducto(p);c.setMateriaPrima(m);
   c.setNombre(item.nombre()==null||item.nombre().isBlank()?m.getNombre():item.nombre().trim());
   c.setPrecioAdicional(Math.max(0,item.precioAdicional()==null?0:item.precioAdicional()));c.setCantidadConsumo(item.cantidadConsumo());c.setActivo(true);nuevos.add(c);
  }
  complementos.saveAll(nuevos); return construirConfiguracion(p);
 }
 private ComplementoProductoDTO dto(ProductoComplemento c){return new ComplementoProductoDTO(c.getId(),c.getMateriaPrima().getCodigo(),c.getNombre(),c.getPrecioAdicional(),c.getCantidadConsumo(),c.getActivo());}
}
