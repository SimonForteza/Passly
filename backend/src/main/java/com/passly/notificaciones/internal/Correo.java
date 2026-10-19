package com.passly.notificaciones.internal;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name="correo", schema="notificaciones")
class Correo {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(nullable=false) String destinatario;
    @Column(nullable=false) String asunto;
    @Column(nullable=false, columnDefinition="text") String cuerpo;
    @Column(nullable=false) String estado = "PENDIENTE";
    int intentos;
    OffsetDateTime proximoIntento = OffsetDateTime.now();
    protected Correo() {}
    Correo(String destinatario, String asunto, String cuerpo) {
        this.destinatario=destinatario; this.asunto=asunto; this.cuerpo=cuerpo;
    }
}
