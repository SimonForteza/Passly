package com.passly.ventas.internal.datos;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Una compra confirmada. <b>Raiz del agregado</b>: los {@link ItemOrden} cuelgan de aca por
 * cascada, igual que {@code TipoEntrada} de {@code Evento} en el modulo Eventos.
 *
 * <p><b>Documento historico, no una vista en vivo.</b> No hay join contra {@code eventos}: cada
 * {@link ItemOrden} guarda el nombre y el precio que tenia el tipo de entrada en el momento de
 * la compra. Si el organizador cambia el precio despues, esta orden no se entera — es
 * exactamente el trade-off que evita que {@code ventas} necesite leer el esquema {@code eventos}
 * para mostrar una orden.
 *
 * <p>{@code compradorId} es un id de {@code usuarios.usuario} <b>sin clave foranea</b>, por la
 * misma regla que {@code Evento.productoraId}: CLAUDE.md 4.8 prohibe unir esquemas de
 * componentes distintos.
 */
@Entity
@Table(name = "orden", schema = "ventas")
public class Orden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comprador_id", nullable = false, updatable = false)
    private Long compradorId;

    @Column(nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal total;

    /**
     * Referencia al cobro aprobado por la pasarela. Es una <i>referencia</i>, no el registro del
     * cobro: cuando exista {@code ServicioDePagos} (PAS-7), el registro va a vivir en su propio
     * esquema. No hay tabla {@code ventas.pago} a proposito, para no crear hoy algo que habria
     * que borrar o duplicar despues.
     */
    @Column(name = "comprobante_cobro", nullable = false, updatable = false, length = 100)
    private String comprobanteCobro;

    @Column(name = "cobrada_en", nullable = false, updatable = false)
    private OffsetDateTime cobradaEn;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private OffsetDateTime creadaEn;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemOrden> items = new ArrayList<>();

    /** Requerido por JPA. No usar desde el codigo de negocio. */
    protected Orden() {
    }

    public Orden(Long compradorId, BigDecimal total, String comprobanteCobro, OffsetDateTime cobradaEn) {
        this.compradorId = compradorId;
        this.total = total;
        this.comprobanteCobro = comprobanteCobro;
        this.cobradaEn = cobradaEn;
        this.creadaEn = OffsetDateTime.now();
    }

    /** Agrega una linea al agregado y mantiene los dos extremos de la relacion sincronizados. */
    public void agregarItem(ItemOrden item) {
        item.asignarA(this);
        this.items.add(item);
    }

    public Long getId() {
        return id;
    }

    public Long getCompradorId() {
        return compradorId;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getComprobanteCobro() {
        return comprobanteCobro;
    }

    public OffsetDateTime getCobradaEn() {
        return cobradaEn;
    }

    public OffsetDateTime getCreadaEn() {
        return creadaEn;
    }

    public List<ItemOrden> getItems() {
        return List.copyOf(items);
    }
}
