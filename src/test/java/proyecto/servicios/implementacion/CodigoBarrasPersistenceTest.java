package proyecto.servicios.implementacion;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import proyecto.entidades.Empresa;
import proyecto.entidades.Producto;
import proyecto.entidades.Sede;
import proyecto.entidades.Inventario;
import proyecto.repositorios.InventarioRepository;
import proyecto.repositorios.ProductoRepository;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {"spring.sql.init.mode=never", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"})
class CodigoBarrasPersistenceTest {
    @Autowired EntityManager em;
    @Autowired ProductoRepository productos;
    @Autowired InventarioRepository inventarios;

    private Empresa empresa(long nit) {
        Empresa e = new Empresa(); e.setNit(nit); e.setNombre("Prueba"); em.persist(e); return e;
    }
    private Producto producto(Empresa e, String codigo) {
        Producto p = new Producto(); p.setEmpresa(e); p.setNombre("Prueba");
        p.setPrecioVenta(1000D); p.setCodigoBarras(codigo); em.persist(p); return p;
    }
    @Test void permiteMismoCodigoEnEmpresasDistintasYProductosSinCodigo() {
        Empresa a = empresa(111L), b = empresa(222L);
        Producto p = producto(a, "00123"); producto(b, "00123");
        producto(a, null); producto(a, null); em.flush();
        assertEquals(p.getCodigo(), productos.findByEmpresaNitAndCodigoBarras(111L, "00123").orElseThrow().getCodigo());
    }
    @Test void indiceImpideDuplicadosEnLaMismaEmpresa() {
        Empresa e = empresa(111L); producto(e, "00123"); em.flush();
        assertThrows(RuntimeException.class, () -> { producto(e, "00123"); em.flush(); });
    }
    @Test void inventarioExponeCodigoSoloParaLaEmpresaDeLaSede() {
        Empresa a = empresa(111L), b = empresa(222L);
        Producto p = producto(a, "00123"); producto(b, "00999");
        Sede sede = new Sede(); sede.setEmpresa(a); em.persist(sede);
        Inventario inv = new Inventario(); inv.setSede(sede); inv.setProducto(p); em.persist(inv); em.flush();
        var lista = inventarios.listarInventarioCompletoPorSede(sede.getId());
        assertEquals(1, lista.size()); assertEquals("00123", lista.get(0).codigoBarras());
    }
}
