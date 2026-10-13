package proyecto.servicios.implementacion;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import proyecto.entidades.*;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {"spring.sql.init.mode=never", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"})
class HistorialMateriaPrimaPersistenceTest {
    @Autowired EntityManager em;

    @Test void actualizacionesConcurrentesNoPierdenStockNiDuplicanHistorial() {
        Sede sede = new Sede(); sede.setUbicacion("Concurrencia"); em.persist(sede);
        MateriaPrima materia = new MateriaPrima(); materia.setNombre("Concurrencia"); em.persist(materia);
        MateriaPrimaSede stock = new MateriaPrimaSede(); stock.setMateriaPrima(materia); stock.setSede(sede);
        stock.cambiarStock(79,"ENTRADA",null,"Inicial"); em.persist(stock); em.flush();
        Long id = stock.getId();
        var factory = em.getEntityManagerFactory();
        org.springframework.test.context.transaction.TestTransaction.flagForCommit();
        org.springframework.test.context.transaction.TestTransaction.end();
        var first = factory.createEntityManager();
        var second = factory.createEntityManager();
        try {
            first.getTransaction().begin(); second.getTransaction().begin();
            var a = first.find(MateriaPrimaSede.class,id);
            var b = second.find(MateriaPrimaSede.class,id);
            a.cambiarStock(78,"VENTA_UNITARIA",297L,"A");
            b.cambiarStock(77,"VENTA_COMBO",893L,"B");
            first.getTransaction().commit();
            assertThrows(jakarta.persistence.RollbackException.class, () -> second.getTransaction().commit());
        } finally {
            if (first.getTransaction().isActive()) first.getTransaction().rollback();
            if (second.getTransaction().isActive()) second.getTransaction().rollback();
            first.close(); second.close();
        }
        org.springframework.test.context.transaction.TestTransaction.start();
        em.clear();
        var actual = em.find(MateriaPrimaSede.class,id);
        assertEquals(78,actual.getCantidadActualMl());
        assertEquals(2,actual.getMovimientos().size());
    }

    @Test void rollbackRevierteExistenciaEHistorial() {
        Sede sede = new Sede(); sede.setUbicacion("Rollback"); em.persist(sede);
        MateriaPrima materia = new MateriaPrima(); materia.setNombre("Rollback"); em.persist(materia);
        MateriaPrimaSede stock = new MateriaPrimaSede(); stock.setMateriaPrima(materia); stock.setSede(sede);
        stock.cambiarStock(79,"ENTRADA",null,"Inicial"); em.persist(stock); em.flush();
        Long id = stock.getId();
        stock.cambiarStock(78,"VENTA_UNITARIA",297L,"Venta"); em.flush();
        org.springframework.test.context.transaction.TestTransaction.flagForRollback();
        org.springframework.test.context.transaction.TestTransaction.end();
        org.springframework.test.context.transaction.TestTransaction.start();
        assertNull(em.find(MateriaPrimaSede.class,id));
        assertEquals(0L,em.createQuery("select count(m) from MovimientoMateriaPrima m where m.inventario.id=:id",Long.class)
                .setParameter("id",id).getSingleResult());
    }

    @Test void stockEHistorialSePersistenJuntosInclusoSinSaveExplicito() {
        Sede sede = new Sede(); sede.setUbicacion("Prueba"); em.persist(sede);
        MateriaPrima materia = new MateriaPrima(); materia.setNombre("Guayaba"); em.persist(materia);
        MateriaPrimaSede stock = new MateriaPrimaSede(); stock.setMateriaPrima(materia); stock.setSede(sede);
        stock.cambiarStock(79,"ENTRADA",null,"Inicial"); em.persist(stock); em.flush();
        Long id = stock.getId(); em.clear();
        stock = em.find(MateriaPrimaSede.class,id);
        stock.cambiarStock(99,"ENTRADA",null,"Recepcion");
        stock.cambiarStock(97,"VENTA_COMBO",893L,"Combo guayaba");
        em.flush(); em.clear();
        MateriaPrimaSede recargado = em.find(MateriaPrimaSede.class,id);
        assertEquals(97,recargado.getCantidadActualMl());
        assertEquals(3,recargado.getMovimientos().size());
        assertEquals(1L,recargado.getVersion());
    }
}
