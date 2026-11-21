package com.passly.eventos.internal.datos;

import com.passly.eventos.EdicionDeEventoInvalidaException;
import com.passly.eventos.EstadoEvento;
import com.passly.eventos.TransicionDeEstadoInvalidaException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * {@link EstadoEvento#PUBLICADO}: en la cartelera y a la venta.
 *
 * <p>La regla de PAS-19: publicado, <b>solo se admite lo que amplia</b> (sumar un tipo de entrada,
 * sumar cupo). Nada que pueda perjudicar a quien ya compro o ya vio la cartelera: cambiar la
 * fecha, el lugar, un precio o bajar un cupo. Republicar es un conflicto, no un no-op.
 */
final class Publicado implements EstadoDeEvento {

    static final Publicado INSTANCIA = new Publicado();

    private Publicado() {
    }

    @Override
    public EstadoEvento valor() {
        return EstadoEvento.PUBLICADO;
    }

    @Override
    public void publicar(Evento evento) {
        throw new TransicionDeEstadoInvalidaException(
                "El evento " + evento.getId() + " esta en estado PUBLICADO"
                        + " y solo se puede publicar un evento en BORRADOR");
    }

    /**
     * {@code PUBLICADO -> CANCELADO}. Los reintegros y el aviso a quienes ya compraron quedan
     * fuera de alcance de PAS-35 (HU-14 en {@code docs/funcional.md}).
     */
    @Override
    public void cancelar(Evento evento) {
        evento.pasarA(Cancelado.INSTANCIA);
    }

    @Override
    public void editarDatos(Evento evento, String nombre, String descripcion, OffsetDateTime fechaHora, String lugar) {
        throw new EdicionDeEventoInvalidaException(
                "No se puede editar los datos del evento " + evento.getId()
                        + " porque esta en estado PUBLICADO y solo se admite en BORRADOR");
    }

    @Override
    public void agregarTipoEntrada(Evento evento, String nombre, BigDecimal precio, Integer cupoTotal) {
        evento.aplicarNuevoTipoEntrada(nombre, precio, cupoTotal);
    }

    @Override
    public void editarTipoEntrada(
            Evento evento, Long idTipoEntrada, String nombre, BigDecimal precio, Integer cupoTotal) {
        throw new EdicionDeEventoInvalidaException(
                "No se puede editar un tipo de entrada del evento " + evento.getId()
                        + " porque esta en estado PUBLICADO y solo se admite en BORRADOR");
    }

    @Override
    public void ampliarCupo(Evento evento, Long idTipoEntrada, int cantidad) {
        evento.aplicarAmpliacionDeCupo(idTipoEntrada, cantidad);
    }

    @Override
    public void cambiarPortada(Evento evento, OffsetDateTime actualizadaEn) {
        evento.aplicarPortada(actualizadaEn);
    }

    @Override
    public boolean admiteVentas() {
        return true;
    }
}
