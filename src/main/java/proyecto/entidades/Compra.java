package proyecto.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "compra", uniqueConstraints = @UniqueConstraint(name = "uk_compra_factura_proveedor_sede", columnNames = {"sede_id", "proveedor_id", "numero_factura"}))
@Getter @Setter @NoArgsConstructor
public class Compra {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "empresa_id", nullable = false) private Empresa empresa;
    @ManyToOne(optional = false) @JoinColumn(name = "sede_id", nullable = false) private Sede sede;
    @ManyToOne(optional = false) @JoinColumn(name = "proveedor_id", nullable = false) private Proveedor proveedor;
    @Column(name = "numero_factura", nullable = false, length = 80) private String numeroFactura;
    @Column(name = "fecha_factura", nullable = false) private LocalDate fechaFactura;
    @Column(name = "fecha_recepcion", nullable = false) private LocalDateTime fechaRecepcion;
    @Column(name = "registrado_por", nullable = false, length = 20) private String registradoPor;
    @Column(name = "registrado_por_id", nullable = false) private Long registradoPorId;
    @Column(name = "condicion_pago", nullable = false, length = 20) private String condicionPago;
    @Column(name = "modo_pago", length = 30) private String modoPago;
    @Column(name = "fecha_vencimiento") private LocalDate fechaVencimiento;
    @Column(nullable = false, precision = 19, scale = 2) private java.math.BigDecimal total;
    @Column(name = "total_pagado", nullable = false, precision = 19, scale = 2) private java.math.BigDecimal totalPagado;
    @Column(nullable = false, length = 20) private String estado = "RECIBIDA";
    @Column(length = 500) private String observacion;
    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true) private List<CompraDetalle> detalles = new ArrayList<>();
    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true) private List<PagoCompra> pagos = new ArrayList<>();
}
