package proyecto.servicios.implementacion;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import proyecto.entidades.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties={"spring.sql.init.mode=never","spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"})
@Import(HistorialProductoService.class)
class HistorialProductoPersistenceTest {
    @Autowired EntityManager em;
    @Autowired HistorialProductoService historial;
    private LocalDate hoy() {return LocalDate.now(ZoneId.of("America/Bogota"));}
    private Inventario crear() {
        Empresa empresa=new Empresa(); empresa.setNit(999123L); empresa.setNombre("QA"); em.persist(empresa);
        Sede sede=new Sede(); sede.setEmpresa(empresa); em.persist(sede);
        Producto producto=new Producto(); producto.setNombre("Producto QA"); producto.setPrecioVenta(3000D);
        producto.setEmpresa(empresa); em.persist(producto);
        Inventario i=new Inventario(); i.setSede(sede); i.setProducto(producto); em.persist(i); em.flush();
        return i;
    }
    @Test void conciliaEntradasVentasPerdidasYStockSinAlterarOtrosInventarios() {
        Inventario i=crear(); long id=i.getId();
        i.cambiarStock(20,"ENTRADA","Ingreso");
        i.cambiarStock(17,"VENTA_UNITARIA","Venta");
        i.cambiarStock(16,"PERDIDA","Perdida");
        i.cambiarStock(14,"SALIDA_MANUAL","Salida");
        em.flush();
        var r=historial.consultar(i.getSede().getId(),i.getProducto().getCodigo(),hoy().minusDays(1),hoy());
        assertFalse(r.conReceta()); assertEquals(14D,r.stockActual());
        assertNull(r.dias().get(0).stockFinal());
        var d=r.dias().get(1);
        assertEquals(20,d.entradas()); assertEquals(3,d.ventaUnitaria());
        assertEquals(1,d.perdidas()); assertEquals(2,d.salidasManuales());
        em.clear(); assertEquals(5,em.find(Inventario.class,id).getMovimientosStock().size());
        assertThrows(IllegalArgumentException.class,()->historial.consultar(99999,i.getProducto().getCodigo(),hoy(),hoy()));
    }
    @Test void productoConRecetaMuestraActividadYEnlazaInsumosSinStockFicticio() {
        Inventario i=crear();
        MateriaPrima mp=new MateriaPrima(); mp.setNombre("Insumo QA"); em.persist(mp);
        MateriaPrimaSede stock=new MateriaPrimaSede(); stock.setMateriaPrima(mp); stock.setSede(i.getSede());
        stock.cambiarStock(79,"ENTRADA",null,"Inicial"); em.persist(stock);
        ProductoMateriaPrima receta=new ProductoMateriaPrima(); receta.setProducto(i.getProducto());
        receta.setMateriaPrima(mp); receta.setMlConsumidos(2); em.persist(receta);
        MovimientoInventario m=new MovimientoInventario(); m.setProducto(i.getProducto());m.setSede(i.getSede());
        m.setTipo(TipoMovimiento.SALIDA);m.setCantidad(3);m.setObservacion("Venta de producto");em.persist(m);em.flush();
        var r=historial.consultar(i.getSede().getId(),i.getProducto().getCodigo(),hoy(),hoy());
        assertTrue(r.conReceta()); assertNull(r.stockActual()); assertNull(r.dias().get(0).stockFinal());
        assertEquals(39D,r.disponibilidad()); assertEquals(3,r.dias().get(0).ventaUnitaria());
        assertEquals(1,r.materiasPrimas().size()); assertEquals(-3,r.movimientos().get(0).cantidad());
    }
}
