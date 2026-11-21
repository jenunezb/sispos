package proyecto.servicios.implementacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import proyecto.controladores.MeseroAdminController;
import proyecto.controladores.MeseroController;
import proyecto.controladores.MeseroValidacionAdvice;
import proyecto.entidades.*;
import proyecto.excepciones.GlobalExceptionHandler;
import proyecto.utils.FiltroToken;
import proyecto.utils.JWTUtils;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DataJpaTest(properties = {
        "spring.sql.init.mode=never",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "jwt.secret=0123456789AB0123456789AB0123456789AB0123456789AB"
}, showSql = false)
@Import({MeseroAdminController.class, MeseroController.class, MeseroCuentaService.class, MeseroPedidoService.class,
        MesaEstadoServicioImpl.class, ComandaCocinaServicioImpl.class, InventarioServicioImpl.class,
        AdministradorAccesoService.class, JWTUtils.class, FiltroToken.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MeseroIntegrationTest {
    @Autowired EntityManager em;
    @Autowired TransactionTemplate tx;
    @Autowired JWTUtils jwt;
    @Autowired FiltroToken filtro;
    @Autowired MeseroAdminController adminController;
    @Autowired MeseroController meseroController;
    @MockBean NotificacionStockMinimoService notificaciones;

    private final ObjectMapper json = new ObjectMapper();
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(adminController, meseroController)
                .setControllerAdvice(new MeseroValidacionAdvice(), new GlobalExceptionHandler())
                .addFilters(filtro)
                .build();
    }

    @Test
    void elMeseroSoloVeSuSedeYElPedidoQuedaEnLaMesaYEnCocina() throws Exception {
        Sede sede = sede(997611L, "Sala principal");
        Sede otraSede = sede(997612L, "Otra sede");
        Long cafe = tx.execute(status -> {
            crearMesa(sede, 2L, true);
            crearMesa(sede, 3L, false);
            crearMesa(otraSede, 4L, true);
            return crearProducto(sede, "Café");
        });
        Long ajeno = tx.execute(status -> crearProducto(otraSede, "Producto ajeno"));

        String base = "/api/administrador/meseros/sede/";
        String admin = jwt.generarToken("admin997611@example.test", Map.of("rol", "administrador"));
        String alta = """
                {"nombre":"Ana Ruiz","usuario":"Ana.Sala","password":"clave-prueba"}
                """;
        enviar(post(base + otraSede.getId()), admin, alta).andExpect(status().isForbidden());
        enviar(post(base + sede.getId()), admin, alta.replace("clave-prueba", "corta"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.respuesta").value("La contraseña debe tener entre 8 y 72 caracteres"));
        String respuesta = enviar(post(base + sede.getId()), admin, alta).andExpect(status().isCreated())
                .andExpect(jsonPath("$.respuesta.usuario").value("ana.sala"))
                .andExpect(jsonPath("$.respuesta.passwordHash").doesNotExist()).andReturn().getResponse().getContentAsString();
        long meseroId = json.readTree(respuesta).at("/respuesta/id").asLong();

        String login = """
                {"usuario":"ANA.SALA","password":"clave-prueba"}
                """;
        String sesion = enviar(post("/api/mesero/login"), null, login).andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.nombre").value("Ana Ruiz")).andReturn().getResponse().getContentAsString();
        String token = json.readTree(sesion).at("/respuesta/token").asText();

        enviar(get("/api/mesero/mesas"), admin, null).andExpect(status().isUnauthorized());
        enviar(get("/api/mesero/mesas"), token, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.length()").value(1)).andExpect(jsonPath("$.respuesta[0].id").value(2));
        enviar(get("/api/mesero/productos"), token, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.length()").value(1)).andExpect(jsonPath("$.respuesta[0].id").value(cafe));
        enviar(post("/api/ventas"), token, "{}").andExpect(status().isForbidden());
        enviar(get("/api/administrador/cajas"), token, null).andExpect(status().isForbidden());
        enviar(get("/api/inventarios/sede/" + otraSede.getId()), token, null).andExpect(status().isForbidden());
        enviar(post("/api/mesero/mesas/4/pedidos"), token, pedido(cafe, 0, true)).andExpect(status().isNotFound());
        enviar(post("/api/mesero/mesas/2/pedidos"), token, pedido(ajeno, 0, true)).andExpect(status().isBadRequest());

        String creado = enviar(post("/api/mesero/mesas/2/pedidos"), token, pedido(cafe, 0, true))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.respuesta.mesa.estado").value("OCUPADA"))
                .andExpect(jsonPath("$.respuesta.mesa.pedido[0].total").value(6000))
                .andExpect(jsonPath("$.respuesta.comandaId").isNumber()).andReturn().getResponse().getContentAsString();
        enviar(post("/api/mesero/mesas/2/pedidos"), token, pedido(cafe, 0, true)).andExpect(status().isConflict());

        long comandaId = json.readTree(creado).at("/respuesta/comandaId").asLong();
        tx.executeWithoutResult(status -> {
            ComandaCocina comanda = em.find(ComandaCocina.class, comandaId);
            assertEquals("Ana Ruiz", comanda.getMesero().getNombre());
            assertEquals("Sin azúcar", comanda.getObservaciones());
            assertEquals(2, comanda.getDetalles().get(0).getCantidad());
            Sede persistida = em.find(Sede.class, sede.getId());
            persistida.setImpresionCocinaHabilitada(false);
        });

        long version = json.readTree(creado).at("/respuesta/mesa/version").asLong();
        enviar(post("/api/mesero/mesas/2/pedidos"), token, pedido(cafe, version, true)).andExpect(status().isBadRequest());
        enviar(get("/api/mesero/mesas"), token, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta[0].pedido.length()").value(1));
        enviar(post("/api/mesero/mesas/2/pedidos"), token, pedido(cafe, version, false))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.respuesta.mesa.pedido.length()").value(2))
                .andExpect(jsonPath("$.respuesta.comandaId").isEmpty());

        enviar(put(base + sede.getId() + "/" + meseroId), admin, "{\"nombre\":\"Ana Ruiz\",\"activo\":false}")
                .andExpect(status().isOk());
        enviar(get("/api/mesero/mesas"), token, null).andExpect(status().isUnauthorized());
        enviar(post("/api/mesero/login"), null, login).andExpect(status().isUnauthorized());
    }

    private String pedido(Long productoId, long version, boolean enviarCocina) {
        return """
                {"version":%d,"productos":[{"productoId":%d,"cantidad":2}],
                 "enviarCocina":%b,"observaciones":"Sin azúcar"}
                """.formatted(version, productoId, enviarCocina);
    }

    private ResultActions enviar(MockHttpServletRequestBuilder req, String token, String contenido) throws Exception {
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (contenido != null) req.contentType(MediaType.APPLICATION_JSON).content(contenido);
        return mvc.perform(req);
    }

    private Sede sede(long nit, String nombre) {
        return tx.execute(status -> {
            Empresa empresa = new Empresa();
            empresa.setNit(nit);
            empresa.setNombre(nombre);
            em.persist(empresa);
            Administrador admin = new Administrador();
            admin.setCorreo("admin" + nit + "@example.test");
            admin.setPassword("prueba");
            admin.setEmpresa(empresa);
            admin.setEsAdministradorEmpresa(true);
            em.persist(admin);
            Sede sede = new Sede();
            sede.setEmpresa(empresa);
            sede.setUbicacion(nombre);
            em.persist(sede);
            return sede;
        });
    }

    private void crearMesa(Sede sede, long id, boolean visible) {
        MesaEstado mesa = new MesaEstado();
        mesa.setSede(em.getReference(Sede.class, sede.getId()));
        mesa.setMesaReferenciaId(id);
        mesa.setNumero((int) id - 1);
        mesa.setTipo("MESA");
        mesa.setVisible(visible);
        mesa.setOrdenVisual((int) id);
        mesa.setEstado("LIBRE");
        mesa.setFechaActualizacion(LocalDateTime.now());
        em.persist(mesa);
    }

    private Long crearProducto(Sede sede, String nombre) {
        Sede administrada = em.getReference(Sede.class, sede.getId());
        Producto producto = new Producto();
        producto.setNombre(nombre);
        producto.setPrecioVenta(3000D);
        producto.setEmpresa(administrada.getEmpresa());
        em.persist(producto);
        Inventario inventario = new Inventario();
        inventario.setSede(administrada);
        inventario.setProducto(producto);
        em.persist(inventario);
        return producto.getCodigo();
    }
}
