package com.passly.accesos.internal.datos;

import com.passly.accesos.dto.ValidacionDTO.Resultado;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "validacion", schema = "accesos")
public class Validacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "ticket_id") private Long ticketId;
    @Column(name = "evento_id", nullable = false) private Long eventoId;
    @Column(name = "validador_id", nullable = false) private Long validadorId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Resultado resultado;
    @Column(nullable = false) private OffsetDateTime momento;
    protected Validacion() {}
    public Validacion(Long ticket, Long evento, Long validador, Resultado resultado) {
        this.ticketId = ticket;
        this.eventoId = evento;
        this.validadorId = validador;
        this.resultado = resultado;
        this.momento = OffsetDateTime.now();
    }
}
