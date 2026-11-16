package proyecto.servicios.implementacion;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import proyecto.dto.*;
import proyecto.entidades.*;
import proyecto.repositorios.*;
import proyecto.utils.JWTUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service @RequiredArgsConstructor
public class CompraService {
    private static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");
    private final CompraRepository compras;
    private final CompraDetalleRepository detalles;
    private final PagoCompraRepository pagos;
    private final ProveedorRepository proveedores;
    private final SedeRepository sedes;
    private final MateriaPrimaRepository materias;
    private final MateriaPrimaSedeRepository stocksMateria;
    private final ProductoRepository productos;
    private final InventarioRepository stocksProducto;
    private final AdministradorAccesoService acceso;
    private final VendedorRepository vendedores;
    private final AdministradorRepository administradores;
    private final EmpresaRepository empresas;
    private final JWTUtils jwt;

    @Transactional
    public Map<String,Object> recibir(String authorization, CompraDTO dto) {
        Identidad actor = identidad(authorization);
        Sede sede = sedes.findByIdForUpdate(dto.sedeId()).orElseThrow(() -> new IllegalArgumentException("Sede no encontrada"));
        validarAcceso(actor, sede.getId());
        if (sede.getEmpresa() == null) throw new IllegalArgumentException("La sede no tiene empresa asociada");
        Proveedor proveedor = proveedores.findByIdAndEmpresaNitAndActivoTrue(dto.proveedorId(), sede.getEmpresa().getNit())
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado en la empresa de la sede"));
        String numero = dto.numeroFactura().trim();
        if (compras.existsBySedeIdAndProveedorIdAndNumeroFacturaIgnoreCase(sede.getId(), proveedor.getId(), numero))
            throw new IllegalArgumentException("Ya existe una compra con esa factura para este proveedor y sede");
        String condicion = dto.condicionPago().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("CONTADO", "CREDITO").contains(condicion)) throw new IllegalArgumentException("La condición de pago debe ser CONTADO o CREDITO");
        BigDecimal total = BigDecimal.ZERO;
        for (CompraDTO.Linea l : dto.items()) {
            validarLinea(l);
            total = total.add(l.cantidad().multiply(l.costoUnitario()).setScale(2, java.math.RoundingMode.HALF_UP));
        }
        total = total.setScale(2, java.math.RoundingMode.HALF_UP);
        if(total.signum() <= 0) throw new IllegalArgumentException("El total de la compra debe ser mayor que cero");
        BigDecimal inicial = dto.pagoInicial().setScale(2, java.math.RoundingMode.HALF_UP);
        if (inicial.compareTo(total) > 0) throw new IllegalArgumentException("El pago inicial no puede superar el total de la compra");
        if (condicion.equals("CONTADO") && inicial.compareTo(total) != 0) throw new IllegalArgumentException("Una compra de contado debe quedar pagada en su totalidad");
        if (condicion.equals("CREDITO") && inicial.compareTo(total) < 0 && dto.fechaVencimiento() == null)
            throw new IllegalArgumentException("Indique la fecha de vencimiento del saldo a crédito");
        String modoPago = modoPago(dto.modoPago(), inicial.signum() > 0);

        Compra c = new Compra(); c.setEmpresa(sede.getEmpresa()); c.setSede(sede); c.setProveedor(proveedor);
        c.setNumeroFactura(numero); c.setFechaFactura(dto.fechaFactura()); c.setFechaRecepcion(LocalDateTime.now(COLOMBIA));
        c.setRegistradoPor(actor.rol); c.setRegistradoPorId(actor.id); c.setCondicionPago(condicion);
        c.setModoPago(inicial.signum() > 0 ? modoPago : null); c.setFechaVencimiento(dto.fechaVencimiento());
        c.setTotal(total); c.setTotalPagado(inicial); c.setEstado(inicial.compareTo(total) == 0 ? "PAGADA" : "PENDIENTE");
        c.setObservacion(dto.observacion() == null ? null : dto.observacion().trim());
        c = compras.saveAndFlush(c);

        for (CompraDTO.Linea l : dto.items()) {
            BigDecimal subtotal = l.cantidad().multiply(l.costoUnitario()).setScale(2, java.math.RoundingMode.HALF_UP);
            CompraDetalle d = new CompraDetalle(); d.setCompra(c); d.setTipoItem(l.tipo().trim().toUpperCase(Locale.ROOT));
            d.setCantidad(l.cantidad()); d.setCostoUnitario(l.costoUnitario()); d.setSubtotal(subtotal);
            d.setPresentacion(l.presentacion()); d.setContenidoPresentacion(l.contenidoPresentacion());
            if (d.getTipoItem().equals("MATERIA_PRIMA")) {
                MateriaPrima mp = materias.findById(l.materiaPrimaId()).orElseThrow(() -> new IllegalArgumentException("Materia prima no encontrada"));
                if (!mp.isActiva() || mp.getEmpresa() == null || !mp.getEmpresa().getNit().equals(sede.getEmpresa().getNit())) throw new IllegalArgumentException("Materia prima no pertenece a la empresa o está inactiva");
                if (mp.getUnidadBase() == null || !mp.getUnidadBase().equalsIgnoreCase(l.unidadBase())) throw new IllegalArgumentException("La unidad debe coincidir con la unidad base de la materia prima");
                d.setMateriaPrima(mp); d.setNombreItem(mp.getNombre()); d.setUnidadBase(mp.getUnidadBase());
                MateriaPrimaSede stock = stocksMateria.findByMateriaPrimaCodigoAndSedeId(mp.getCodigo(), sede.getId()).orElseGet(() -> {
                    MateriaPrimaSede m = new MateriaPrimaSede(); m.setMateriaPrima(mp); m.setSede(sede); m.setActiva(true); m.setCantidadActualMl(0); m.setMlPorVaso(0); return m;
                });
                double anteriorTotal = stocksMateria.sumarStockMateriaPrima(mp.getCodigo());
                double cantidad = l.cantidad().doubleValue(), costo = l.costoUnitario().doubleValue();
                double anteriorCosto = mp.getCostoUnitario() == null ? 0 : mp.getCostoUnitario();
                double promedio = anteriorTotal > 0 && anteriorCosto > 0 ? ((anteriorTotal * anteriorCosto) + (cantidad * costo)) / (anteriorTotal + cantidad) : costo;
                mp.setCostoUnitario(promedio); materias.save(mp);
                stock.cambiarStock(stock.getCantidadActualMl() + cantidad, "ENTRADA", null, "Compra #" + c.getId() + " / factura " + numero);
                stock.getMovimientos().get(stock.getMovimientos().size()-1).setCompraId(c.getId());
                stocksMateria.save(stock);
            } else {
                Producto p = productos.findByCodigoAndEmpresaNit(l.productoId(), sede.getEmpresa().getNit()).orElseThrow(() -> new IllegalArgumentException("Producto no encontrado en la empresa"));
                if (p.getActivo() == null || !p.getActivo() || !p.getMateriasPrimas().isEmpty()) throw new IllegalArgumentException("Solo se pueden recibir productos activos para reventa, sin receta");
                if (l.cantidad().stripTrailingZeros().scale() > 0) throw new IllegalArgumentException("La cantidad de producto terminado debe ser un número entero");
                int cantidad = l.cantidad().intValueExact();
                d.setProducto(p); d.setNombreItem(p.getNombre()); d.setUnidadBase(l.unidadBase() == null ? "UNIDAD" : l.unidadBase());
                Inventario stock = stocksProducto.findVisibleByProductoCodigoAndSedeId(p.getCodigo(), sede.getId()).orElseGet(() -> {
                    Inventario i = new Inventario(); i.setProducto(p); i.setSede(sede); i.setStockActual(0); i.setEntradas(0); i.setSalidas(0); i.setPerdidas(0); i.setStockMinimo(0); i.setAlertaStockMinimoActiva(false); return i;
                });
                BigDecimal anterior = p.getCostoCompraPromedio() == null ? BigDecimal.ZERO : p.getCostoCompraPromedio();
                int anteriorStock = stock.getStockActual();
                BigDecimal promedio = anteriorStock > 0 && anterior.signum() > 0
                        ? anterior.multiply(BigDecimal.valueOf(anteriorStock)).add(l.costoUnitario().multiply(BigDecimal.valueOf(cantidad))).divide(BigDecimal.valueOf(anteriorStock + cantidad), 4, java.math.RoundingMode.HALF_UP)
                        : l.costoUnitario().setScale(4, java.math.RoundingMode.HALF_UP);
                p.setCostoCompraPromedio(promedio); productos.save(p);
                stock.setEntradas(stock.getEntradas() + cantidad);
                stock.cambiarStock(stock.getStockActual() + cantidad, "ENTRADA", "Compra #" + c.getId() + " / factura " + numero);
                stock.getMovimientosStock().get(stock.getMovimientosStock().size()-1).setCompraId(c.getId());
                stocksProducto.save(stock);
            }
            detalles.save(d);
        }
        if (inicial.signum() > 0) {
            PagoCompra pago = new PagoCompra(); pago.setCompra(c); pago.setFecha(LocalDate.now(COLOMBIA)); pago.setMonto(inicial);
            pago.setModoPago(modoPago); pago.setReferencia(dto.referenciaPago()); pago.setRegistradoPor(actor.rol); pago.setRegistradoPorId(actor.id); pagos.save(pago);
        }
        return detalle(c);
    }

    @Transactional(readOnly=true)
    public List<Map<String,Object>> listar(String authorization, Long sedeId, Long empresaNit, LocalDate desde, LocalDate hasta) {
        Identidad actor = identidad(authorization);
        if (actor.rol.equals("vendedor")) {
            Vendedor v = vendedores.findByCodigo(actor.id.intValue()).orElseThrow(() -> new IllegalArgumentException("Vendedor no encontrado"));
            if (v.getSede() == null) throw new IllegalArgumentException("El vendedor no tiene sede");
            if (sedeId != null && !sedeId.equals(v.getSede().getId())) throw new IllegalArgumentException("No puede consultar otra sede");
            sedeId = v.getSede().getId();
            return compras.buscar(sedeId, desde, hasta).stream().map(this::detalle).toList();
        }
        Administrador admin=administradores.findByCodigo(actor.id.intValue()).orElseThrow();
        if (sedeId != null) {
            acceso.validarAccesoASede(admin, sedeId);
            return compras.buscar(sedeId, desde, hasta).stream().map(this::detalle).toList();
        }
        List<Sede> permitidas;
        if(admin.isEsSuperAdmin()) {
            if(empresaNit==null) throw new IllegalArgumentException("El super administrador debe indicar empresaNit o sedeId");
            permitidas=sedes.findByEmpresaNit(empresaNit);
        } else permitidas=acceso.obtenerSedesVisibles(admin);
        List<Long> ids=permitidas.stream().map(Sede::getId).toList();
        if(ids.isEmpty()) return List.of();
        return compras.buscarPorSedes(ids, desde, hasta).stream().map(this::detalle).toList();
    }

    @Transactional
    public Map<String,Object> abonar(String authorization, Long compraId, PagoCompraDTO dto) {
        Identidad actor = identidad(authorization);
        if (!actor.rol.equals("administrador")) throw new IllegalArgumentException("Solo un administrador puede registrar abonos");
        Compra c = compras.findById(compraId).orElseThrow(() -> new IllegalArgumentException("Compra no encontrada"));
        acceso.validarAccesoASede(administradores.findByCodigo(actor.id.intValue()).orElseThrow(), c.getSede().getId());
        if (!Set.of("EFECTIVO", "TRANSFERENCIA", "MIXTO").contains(dto.modoPago().toUpperCase(Locale.ROOT))) throw new IllegalArgumentException("Medio de pago inválido");
        BigDecimal saldo = c.getTotal().subtract(c.getTotalPagado());
        if (dto.monto().compareTo(BigDecimal.ZERO) <= 0 || dto.monto().compareTo(saldo) > 0) throw new IllegalArgumentException("El abono debe ser mayor que cero y no superar el saldo pendiente");
        PagoCompra p = new PagoCompra(); p.setCompra(c); p.setFecha(dto.fecha()); p.setMonto(dto.monto().setScale(2, java.math.RoundingMode.HALF_UP));
        p.setModoPago(dto.modoPago().toUpperCase(Locale.ROOT)); p.setReferencia(dto.referencia()); p.setRegistradoPor(actor.rol); p.setRegistradoPorId(actor.id); pagos.save(p);
        c.setTotalPagado(c.getTotalPagado().add(p.getMonto())); if (c.getTotalPagado().compareTo(c.getTotal()) == 0) c.setEstado("PAGADA");
        compras.save(c); return detalle(c);
    }

    @Transactional(readOnly=true)
    public List<Map<String,Object>> listarProveedores(String authorization, Long empresaNit) {
        Identidad actor = identidad(authorization); Long nit = empresaNit;
        if (actor.rol.equals("vendedor")) {
            Vendedor v = vendedores.findByCodigo(actor.id.intValue()).orElseThrow(); nit = v.getEmpresa()!=null ? v.getEmpresa().getNit() : v.getSede().getEmpresa().getNit();
        } else {
            Administrador a = administradores.findByCodigo(actor.id.intValue()).orElseThrow();
            if (!a.isEsSuperAdmin()) nit = a.getEmpresa().getNit();
            else if (nit == null) throw new IllegalArgumentException("Indique empresaNit");
        }
        Long company = nit;
        return proveedores.findByEmpresaNitAndActivoTrueOrderByNombreAsc(company).stream().map(this::proveedorMap).toList();
    }

    @Transactional
    public Map<String,Object> crearProveedor(String authorization, Long empresaNit, ProveedorDTO dto) {
        Identidad actor=identidad(authorization); if (!actor.rol.equals("administrador")) throw new IllegalArgumentException("Solo un administrador puede crear proveedores");
        Administrador a=administradores.findByCodigo(actor.id.intValue()).orElseThrow();
        Empresa e;
        if (a.isEsSuperAdmin()) e=empresas.findById(Objects.requireNonNull(empresaNit,"Indique empresaNit")).orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada"));
        else { e=a.getEmpresa(); if(e==null) throw new IllegalArgumentException("Administrador sin empresa"); }
        Proveedor p=new Proveedor(); p.setEmpresa(e); p.setNombre(dto.nombre().trim()); p.setIdentificacion(dto.identificacion()); p.setContacto(dto.contacto()); p.setCorreo(dto.correo()); p.setTelefono(dto.telefono()); p.setActivo(true);
        return proveedorMap(proveedores.save(p));
    }

    private void validarLinea(CompraDTO.Linea l) {
        String tipo=l.tipo().trim().toUpperCase(Locale.ROOT);
        if (tipo.equals("MATERIA_PRIMA")) { if(l.materiaPrimaId()==null || l.productoId()!=null || l.unidadBase()==null) throw new IllegalArgumentException("La línea de materia prima requiere materiaPrimaId y unidadBase"); }
        else if (tipo.equals("PRODUCTO")) { if(l.productoId()==null || l.materiaPrimaId()!=null) throw new IllegalArgumentException("La línea de producto requiere productoId"); }
        else throw new IllegalArgumentException("Tipo de artículo inválido: use MATERIA_PRIMA o PRODUCTO");
        if(l.cantidad().signum()<=0 || l.costoUnitario().signum()<0) throw new IllegalArgumentException("Cantidad o costo inválido");
    }
    private String modoPago(String modo, boolean tienePago) { if(!tienePago) return null; if(modo==null || !Set.of("EFECTIVO","TRANSFERENCIA","MIXTO").contains(modo.toUpperCase(Locale.ROOT))) throw new IllegalArgumentException("Indique un medio de pago válido"); return modo.toUpperCase(Locale.ROOT); }
    private Identidad identidad(String auth) { Jws<Claims> claims=jwt.parseJwt(auth.replace("Bearer ","")); String rol=String.valueOf(claims.getBody().get("rol")); String correo=claims.getBody().getSubject();
        if(rol.equals("vendedor")) { Vendedor v=vendedores.findByCorreoIgnoreCase(correo).orElseThrow(() -> new IllegalArgumentException("Vendedor no encontrado")); return new Identidad(rol, (long)v.getCodigo()); }
        Administrador a=acceso.obtenerAdministradorAutenticado(auth); return new Identidad("administrador",(long)a.getCodigo()); }
    private void validarAcceso(Identidad actor, Long sede) { if(actor.rol.equals("administrador")) acceso.validarAccesoASede(administradores.findByCodigo(actor.id.intValue()).orElseThrow(),sede); else { Vendedor v=vendedores.findByCodigo(actor.id.intValue()).orElseThrow(); if(v.getSede()==null || !v.getSede().getId().equals(sede)) throw new IllegalArgumentException("El vendedor solo puede recibir compras en su sede"); } }
    private Map<String,Object> proveedorMap(Proveedor p) { return Map.of("id",p.getId(),"nombre",p.getNombre(),"identificacion",Objects.toString(p.getIdentificacion(),""),"contacto",Objects.toString(p.getContacto(),""),"correo",Objects.toString(p.getCorreo(),""),"telefono",Objects.toString(p.getTelefono(),"")); }
    private Map<String,Object> detalle(Compra c) { Map<String,Object> m=new LinkedHashMap<>(); m.put("id",c.getId());m.put("sedeId",c.getSede().getId());m.put("sede",c.getSede().getUbicacion());m.put("proveedor",proveedorMap(c.getProveedor()));m.put("numeroFactura",c.getNumeroFactura());m.put("fechaFactura",c.getFechaFactura());m.put("fechaRecepcion",c.getFechaRecepcion());m.put("registradoPor",c.getRegistradoPor());m.put("condicionPago",c.getCondicionPago());m.put("modoPagoInicial",c.getModoPago());m.put("fechaVencimiento",c.getFechaVencimiento());m.put("total",c.getTotal());m.put("totalPagado",c.getTotalPagado());m.put("saldo",c.getTotal().subtract(c.getTotalPagado()));m.put("estado",c.getEstado());m.put("observacion",c.getObservacion());
        m.put("items",detalles.findByCompraId(c.getId()).stream().map(d->{Map<String,Object> x=new LinkedHashMap<>();x.put("tipo",d.getTipoItem());x.put("itemId",d.getTipoItem().equals("MATERIA_PRIMA")?d.getMateriaPrima().getCodigo():d.getProducto().getCodigo());x.put("nombre",d.getNombreItem());x.put("unidad",d.getUnidadBase());x.put("cantidad",d.getCantidad());x.put("costoUnitario",d.getCostoUnitario());x.put("subtotal",d.getSubtotal());x.put("presentacion",d.getPresentacion());x.put("contenidoPresentacion",d.getContenidoPresentacion());return x;}).toList());
        m.put("pagos",pagos.findByCompraIdOrderByFechaAscIdAsc(c.getId()).stream().map(p->Map.of("fecha",p.getFecha(),"monto",p.getMonto(),"modoPago",p.getModoPago(),"referencia",Objects.toString(p.getReferencia(),""))).toList()); return m; }
    private record Identidad(String rol, Long id) {}
}
