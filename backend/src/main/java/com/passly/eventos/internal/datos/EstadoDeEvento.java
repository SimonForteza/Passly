package com.passly.eventos.internal.datos;

import com.passly.eventos.EstadoEvento;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * <b>Patron State</b> sobre {@link Evento} (PAS-35): cada estado del ciclo de vida es una clase
 * que decide que operaciones admite. Lo que no admite lo rechaza con la excepcion que la capa web
 * traduce a <b>409 Conflict</b> ({@code TransicionDeEstadoInvalidaException} para las
 * transiciones, {@code EdicionDeEventoInvalidaException} para las ediciones).
 *
 * <p><b>Enum en la base, comportamiento en clases.</b> La columna {@code eventos.evento.estado}
 * sigue guardando el {@link EstadoEvento} como STRING: es el dato estable y el que viaja en el
 * contrato publico. Este objeto no se persiste; {@link #de(EstadoEvento)} lo resuelve a partir del
 * enum cada vez que hace falta. Asi el esquema no cambia, no hace falta un converter JPA y
 * renombrar o reorganizar estas clases no rompe ninguna fila guardada.
 *
 * <p>Los estados no tienen atributos: son singletons, y todo lo que necesitan del evento lo
 * reciben por parametro. Aplican el cambio llamando a los mutadores package-private de
 * {@link Evento} (por eso viven en el mismo paquete), que no vuelven a chequear el estado.
 */
sealed interface EstadoDeEvento permits Borrador, Publicado, Cancelado {

    /**
     * Traduce el dato persistido al comportamiento. El {@code switch} es exhaustivo sobre el enum:
     * si alguien agrega un estado a {@link EstadoEvento} sin su clase, esto deja de compilar.
     */
    static EstadoDeEvento de(EstadoEvento estado) {
        return switch (estado) {
            case BORRADOR -> Borrador.INSTANCIA;
            case PUBLICADO -> Publicado.INSTANCIA;
            case CANCELADO -> Cancelado.INSTANCIA;
        };
    }

    /** El enum que se guarda en la base para este estado. */
    EstadoEvento valor();

    void publicar(Evento evento);

    void cancelar(Evento evento);

    void editarDatos(Evento evento, String nombre, String descripcion, OffsetDateTime fechaHora, String lugar);

    void agregarTipoEntrada(Evento evento, String nombre, BigDecimal precio, Integer cupoTotal);

    void editarTipoEntrada(Evento evento, Long idTipoEntrada, String nombre, BigDecimal precio, Integer cupoTotal);

    void ampliarCupo(Evento evento, Long idTipoEntrada, int cantidad);

    /** Si se pueden vender entradas del evento en este estado. */
    boolean admiteVentas();
}
