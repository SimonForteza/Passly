package com.passly.ventas.internal.datos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Una linea de una {@link Orden} confirmada: snapshot del tipo de entrada al momento de la
 * compra, no una referencia en vivo.
 *
 * <p>No es raiz de agregado: se crea, se persiste y se borra a traves de su {@link Orden}, igual
 * que {@code TipoEntrada} respecto de {@code Evento}.
 */
@Entity
@Table(name = "item_orden", schema = "ventas")
public class ItemOrden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private Orden orden;

    /** Id de {@code eventos.tipo_entrada}, sin clave foranea (CLAUDE.md 4.8). */
    @Column(name = "tipo_entrada_id", nullable = false, updatable = false)
    private Long tipoEntradaId;

    /** Id de {@code eventos.evento}, sin clave foranea. Cada linea sabe de que evento es. */
    @Column(name = "evento_id", nullable = false, updatable = false)
    private Long eventoId;

    @Column(name = "nombre_tipo_entrada", nullable = false, length = 80, updatable = false)
    private String nombreTipoEntrada;

    /** {@code numeric(12,2)} y {@code BigDecimal}: es dinero, nunca {@code double}. */
    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal precioUnitario;

    @Column(nullable = false, updatable = false)
    private Integer cantidad;

    /** Requerido por JPA. No usar desde el codigo de negocio. */
    protected ItemOrden() {
    }

    public ItemOrden(
            Long tipoEntradaId, Long eventoId, String nombreTipoEntrada,
            BigDecimal precioUnitario, Integer cantidad
    ) {
        this.tipoEntradaId = tipoEntradaId;
        this.eventoId = eventoId;
        this.nombreTipoEntrada = nombreTipoEntrada;
        this.precioUnitario = precioUnitario;
        this.cantidad = cantidad;
    }

    void asignarA(Orden orden) {
        this.orden = orden;
    }

    public Long getId() {
        return id;
    }

    public Orden getOrden() {
        return orden;
    }

    public Long getTipoEntradaId() {
        return tipoEntradaId;
    }

    public Long getEventoId() {
        return eventoId;
    }

    public String getNombreTipoEntrada() {
        return nombreTipoEntrada;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public Integer getCantidad() {
        return cantidad;
    }
}
