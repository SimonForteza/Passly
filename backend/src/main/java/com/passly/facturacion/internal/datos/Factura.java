package com.passly.facturacion.internal.datos;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "factura", schema = "facturacion", uniqueConstraints = @UniqueConstraint(columnNames = "orden_id"))
public class Factura {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "orden_id", nullable = false) private Long ordenId;
    @Column(name = "comprador_id", nullable = true) private Long compradorId;
    @Column(name = "comprador_nombre", length=150) private String compradorNombre;
    @Column(name = "comprador_email", length=254) private String compradorEmail;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal total;
    @Column(name = "cobrada_en", nullable = false) private OffsetDateTime cobradaEn;
    @Column(nullable = false, length = 30) private String estado;
    @Column(name = "creada_en", nullable = false) private OffsetDateTime creadaEn;
    protected Factura() {}
}
