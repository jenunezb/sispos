package proyecto.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "materia_prima_sede",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"materia_prima_id", "sede_id"})
        })
@Getter
@Setter
public class MateriaPrimaSede {

    @Version
    private Long version;

    @OneToMany(mappedBy = "inventario", cascade = CascadeType.ALL)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private java.util.List<MovimientoMateriaPrima> movimientos = new java.util.ArrayList<>();

    @PrePersist
    void registrarApertura() {
        if (!movimientos.isEmpty()) return;
        MovimientoMateriaPrima apertura = new MovimientoMateriaPrima();
        apertura.setInventario(this);
        apertura.setFecha(java.time.LocalDateTime.now(java.time.ZoneId.of("America/Bogota")));
        apertura.setTipo("APERTURA");
        apertura.setStockAnterior(cantidadActualMl);
        apertura.setStockNuevo(cantidadActualMl);
        apertura.setObservacion("Inicio del historial");
        movimientos.add(apertura);
    }

    public void setCantidadActualMl(double cantidad) {
        cambiarStock(cantidad, "AJUSTE", null, "Actualizacion de existencia");
    }

    public void cambiarStock(double cantidad, String tipo, Long productoId, String observacion) {
        if (!Double.isFinite(cantidad) || cantidad < 0) {
            throw new IllegalArgumentException("La existencia debe ser un numero finito no negativo");
        }
        if (Double.compare(cantidadActualMl, cantidad) == 0) return;
        MovimientoMateriaPrima movimiento = new MovimientoMateriaPrima();
        movimiento.setInventario(this);
        movimiento.setFecha(java.time.LocalDateTime.now(java.time.ZoneId.of("America/Bogota")));
        movimiento.setTipo(tipo);
        movimiento.setStockAnterior(cantidadActualMl);
        movimiento.setStockNuevo(cantidad);
        movimiento.setProductoId(productoId);
        movimiento.setObservacion(observacion);
        movimientos.add(movimiento);
        cantidadActualMl = cantidad;
    }

    public void consumirVenta(double cantidad, Producto producto) {
        boolean combo = producto.getNombre().toLowerCase(java.util.Locale.ROOT).contains("combo");
        cambiarStock(cantidadActualMl - cantidad, combo ? "VENTA_COMBO" : "VENTA_UNITARIA",
                producto.getCodigo(), producto.getNombre());
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "materia_prima_id")
    private MateriaPrima materiaPrima;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sede_id")
    private Sede sede;

    @Column(nullable = false)
    private double cantidadActualMl;

    @Column(nullable = false)
    private double mlPorVaso;

    @Column(nullable = false)
    private boolean activa = true;
}

