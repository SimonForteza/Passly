package com.passly.eventos.internal.datos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * La imagen de portada de un evento (PAS-47), en {@code eventos.portada}.
 *
 * <p><b>Por que en la base y no en disco o en un bucket.</b> El sistema se despliega como una sola
 * unidad contra un Postgres (Docker en desarrollo, Supabase despues). Guardar los bytes aca no suma
 * infraestructura, viaja con los backups y entra en la misma transaccion que marca el evento. El
 * costo es el tamaño de la base, y por eso el limite de 2 MB por imagen. Si las portadas crecieran,
 * la evolucion es mover el contenido a un almacenamiento de objetos sin tocar el contrato:
 * {@code EventoService} ya entrega la imagen como bytes, no como una fila.
 *
 * <p><b>Tabla aparte de {@code evento}</b> para que listar la cartelera no lea imagenes. La clave
 * es el id del evento, sin generar y sin FK, igual que el resto de las referencias del modulo.
 */
@Entity
@Table(name = "portada", schema = "eventos")
public class Portada {

    @Id
    @Column(name = "evento_id")
    private Long eventoId;

    /** {@code bytea} en Postgres. No es {@code @Lob}: con Hibernate eso iria a large objects (oid). */
    @Column(nullable = false)
    private byte[] contenido;

    /** Detectado de los primeros bytes del archivo, nunca del header que manda el cliente. */
    @Column(name = "tipo_contenido", nullable = false, length = 30)
    private String tipoContenido;

    @Column(name = "actualizada_en", nullable = false)
    private OffsetDateTime actualizadaEn;

    /** Requerido por JPA. */
    protected Portada() {
    }

    public Portada(Long eventoId, byte[] contenido, String tipoContenido, OffsetDateTime actualizadaEn) {
        this.eventoId = eventoId;
        reemplazar(contenido, tipoContenido, actualizadaEn);
    }

    public void reemplazar(byte[] contenido, String tipoContenido, OffsetDateTime actualizadaEn) {
        this.contenido = contenido;
        this.tipoContenido = tipoContenido;
        this.actualizadaEn = actualizadaEn;
    }

    public Long getEventoId() {
        return eventoId;
    }

    public byte[] getContenido() {
        return contenido;
    }

    public String getTipoContenido() {
        return tipoContenido;
    }

    public OffsetDateTime getActualizadaEn() {
        return actualizadaEn;
    }
}
