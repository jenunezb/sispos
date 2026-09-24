package proyecto.entidades;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @ToString
@Entity
public class Inventario {
    @Version private Long version;

    @OneToMany(mappedBy = "inventario", cascade = CascadeType.ALL)
    @com.fasterxml.jackson.annotation.JsonIgnore @ToString.Exclude
    private java.util.List<MovimientoStockProducto> movimientosStock = new java.util.ArrayList<>();

    @PrePersist
    void registrarApertura() {
        if (movimientosStock.isEmpty()) registrarStock(stockActual, "APERTURA", "Inicio del historial");
    }

    public void setStockActual(Integer cantidad) {
        cambiarStock(cantidad, "AJUSTE", "Actualizacion de existencia");
    }

    public void cambiarStock(Integer cantidad, String tipo, String observacion) {
        if (cantidad == null || cantidad < 0) throw new IllegalArgumentException("Existencia invalida");
        if (cantidad.equals(stockActual)) return;
        registrarStock(cantidad, tipo, observacion);
    }

    private void registrarStock(int cantidad, String tipo, String observacion) {
        MovimientoStockProducto m = new MovimientoStockProducto();
        m.setInventario(this);
        m.setFecha(java.time.LocalDateTime.now(java.time.ZoneId.of("America/Bogota")));
        m.setTipo(tipo);
        m.setStockAnterior(stockActual);
        m.setStockNuevo(cantidad);
        m.setObservacion(observacion);
        movimientosStock.add(m);
        stockActual = cantidad;
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer stockActual = 0;

    @Column(nullable = false)
    private Integer entradas = 0;

    @Column(nullable = false)
    private Integer salidas = 0;

    @Column(nullable = false)
    private Integer perdidas = 0;

    @Column(nullable = false)
    private Integer stockMinimo = 0;

    @Column(nullable = false)
    private Boolean alertaStockMinimoActiva = false;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;
}

