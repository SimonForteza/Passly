package com.passly.tickets.internal.datos;

import com.passly.tickets.EstadoTicket;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Ticket individual emitido a partir de una unidad comprada. */
@Entity
@Table(name = "ticket", schema = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID codigo;

    @Column(name = "orden_id", nullable = false, updatable = false)
    private Long ordenId;

    @Column(name = "comprador_id", nullable = false, updatable = false)
    private Long compradorId;

    @Column(name = "evento_id", nullable = false, updatable = false)
    private Long eventoId;

    @Column(name = "tipo_entrada_id", nullable = false, updatable = false)
    private Long tipoEntradaId;

    @Column(name = "nombre_tipo_entrada", nullable = false, length = 80, updatable = false)
    private String nombreTipoEntrada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTicket estado;

    @Column(name = "contenido_qr", nullable = false, unique = true, length = 180, updatable = false)
    private String contenidoQr;

    @Column(name = "emitido_en", nullable = false, updatable = false)
    private OffsetDateTime emitidoEn;

    protected Ticket() {
    }

    public Ticket(
            UUID codigo,
            Long ordenId,
            Long compradorId,
            Long eventoId,
            Long tipoEntradaId,
            String nombreTipoEntrada,
            String contenidoQr
    ) {
        this.codigo = codigo;
        this.ordenId = ordenId;
        this.compradorId = compradorId;
        this.eventoId = eventoId;
        this.tipoEntradaId = tipoEntradaId;
        this.nombreTipoEntrada = nombreTipoEntrada;
        this.contenidoQr = contenidoQr;
        this.estado = EstadoTicket.EMITIDO;
        this.emitidoEn = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public UUID getCodigo() { return codigo; }
    public Long getOrdenId() { return ordenId; }
    public Long getCompradorId() { return compradorId; }
    public Long getEventoId() { return eventoId; }
    public Long getTipoEntradaId() { return tipoEntradaId; }
    public String getNombreTipoEntrada() { return nombreTipoEntrada; }
    public EstadoTicket getEstado() { return estado; }
    public String getContenidoQr() { return contenidoQr; }
    public OffsetDateTime getEmitidoEn() { return emitidoEn; }
}
