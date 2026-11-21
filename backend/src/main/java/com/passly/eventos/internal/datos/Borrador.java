package com.passly.eventos.internal.datos;

import com.passly.eventos.EstadoEvento;
import com.passly.eventos.TransicionDeEstadoInvalidaException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * {@link EstadoEvento#BORRADOR}: recien creado, todavia no anunciado. Admite cualquier edicion,
 * se puede publicar (si cumple las reglas) o cancelar. No admite ventas: la fiesta todavia no esta
 * en la cartelera.
 */
final class Borrador implements EstadoDeEvento {

    static final Borrador INSTANCIA = new Borrador();

    private Borrador() {
    }

    @Override
    public EstadoEvento valor() {
        return EstadoEvento.BORRADOR;
    }

    /**
     * Transicion {@code BORRADOR -> PUBLICADO}. Que el evento este en borrador ya lo garantiza que
     * esta clase sea la que responde; quedan las dos reglas sobre sus datos:
     * <ol>
     *   <li>no se publica un evento cuya fecha ya paso;</li>
     *   <li>no se publica un evento sin al menos un tipo de entrada con cupo.</li>
     * </ol>
     */
    @Override
    public void publicar(Evento evento) {
        if (evento.getFechaHora().isBefore(OffsetDateTime.now())) {
            throw new TransicionDeEstadoInvalidaException(
                    "No se puede publicar el evento " + evento.getId() + " porque su fecha ya paso");
        }
        if (!evento.tieneAlgunTipoDeEntradaConCupo()) {
            throw new TransicionDeEstadoInvalidaException(
                    "No se puede publicar el evento " + evento.getId()
                            + " porque no tiene ningun tipo de entrada con cupo disponible");
        }
        evento.pasarA(Publicado.INSTANCIA);
    }

    /** {@code BORRADOR -> CANCELADO}: se descarta una fiesta que nunca se anuncio. */
    @Override
    public void cancelar(Evento evento) {
        evento.pasarA(Cancelado.INSTANCIA);
    }

    @Override
    public void editarDatos(Evento evento, String nombre, String descripcion, OffsetDateTime fechaHora, String lugar) {
        evento.aplicarDatos(nombre, descripcion, fechaHora, lugar);
    }

    @Override
    public void agregarTipoEntrada(Evento evento, String nombre, BigDecimal precio, Integer cupoTotal) {
        evento.aplicarNuevoTipoEntrada(nombre, precio, cupoTotal);
    }

    @Override
    public void editarTipoEntrada(
            Evento evento, Long idTipoEntrada, String nombre, BigDecimal precio, Integer cupoTotal) {
        evento.aplicarEdicionDeTipoEntrada(idTipoEntrada, nombre, precio, cupoTotal);
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
        return false;
    }
}
