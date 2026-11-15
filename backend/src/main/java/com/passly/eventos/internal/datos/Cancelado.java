package com.passly.eventos.internal.datos;

import com.passly.eventos.EdicionDeEventoInvalidaException;
import com.passly.eventos.EstadoEvento;
import com.passly.eventos.TransicionDeEstadoInvalidaException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * {@link EstadoEvento#CANCELADO}: estado terminal. No admite ninguna transicion, ninguna edicion
 * ni ventas. Sigue visible en el detalle publico (PAS-25): quien compro tiene que poder ver el
 * evento de su orden, y quien abre el link tiene que enterarse de que se cancelo.
 */
final class Cancelado implements EstadoDeEvento {

    static final Cancelado INSTANCIA = new Cancelado();

    private Cancelado() {
    }

    @Override
    public EstadoEvento valor() {
        return EstadoEvento.CANCELADO;
    }

    @Override
    public void publicar(Evento evento) {
        throw new TransicionDeEstadoInvalidaException(
                "El evento " + evento.getId() + " esta en estado CANCELADO"
                        + " y solo se puede publicar un evento en BORRADOR");
    }

    /** Cancelar dos veces es un conflicto, no un no-op: mismo criterio que republicar. */
    @Override
    public void cancelar(Evento evento) {
        throw new TransicionDeEstadoInvalidaException(
                "El evento " + evento.getId() + " ya esta CANCELADO");
    }

    @Override
    public void editarDatos(Evento evento, String nombre, String descripcion, OffsetDateTime fechaHora, String lugar) {
        throw rechazo("editar los datos", evento);
    }

    @Override
    public void agregarTipoEntrada(Evento evento, String nombre, BigDecimal precio, Integer cupoTotal) {
        throw rechazo("agregar un tipo de entrada", evento);
    }

    @Override
    public void editarTipoEntrada(
            Evento evento, Long idTipoEntrada, String nombre, BigDecimal precio, Integer cupoTotal) {
        throw rechazo("editar un tipo de entrada", evento);
    }

    @Override
    public void ampliarCupo(Evento evento, Long idTipoEntrada, int cantidad) {
        throw rechazo("ampliar el cupo de un tipo de entrada", evento);
    }

    @Override
    public boolean admiteVentas() {
        return false;
    }

    private static EdicionDeEventoInvalidaException rechazo(String accion, Evento evento) {
        return new EdicionDeEventoInvalidaException(
                "No se puede " + accion + " del evento " + evento.getId() + " porque esta CANCELADO");
    }
}
