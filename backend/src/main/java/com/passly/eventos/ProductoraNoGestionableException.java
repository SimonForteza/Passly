package com.passly.eventos;

/**
 * Quien opera no puede gestionar los eventos de esa productora.
 *
 * <p>Cubre los dos casos en los que se pierde esa autorizacion, con mensajes distintos: crear un
 * evento a nombre de una productora que no se gestiona, y operar sobre un evento que pertenece a
 * otra. Se mapea a <b>403 Forbidden</b>.
 *
 * <p><b>Por que una sola clase y no dos:</b> el motivo del rechazo es el mismo — no tener permiso de
 * gestion sobre esa productora — y lo que cambia es solo el contexto, que va en el mensaje. Dos
 * clases obligarian a dos handlers identicos.
 *
 * <p><b>Por que es propia de Eventos</b> y no se reutiliza la de Productoras: si Eventos propagara
 * {@code NoEsMiembroDeLaProductoraException}, su API publica pasaria a incluir un tipo de otro
 * modulo, y quien consuma Eventos tendria que conocer el contrato de Productoras para capturarla.
 */
public class ProductoraNoGestionableException extends RuntimeException {

    private final Long idProductora;
    private final Long idUsuario;

    private ProductoraNoGestionableException(String mensaje, Long idProductora, Long idUsuario) {
        super(mensaje);
        this.idProductora = idProductora;
        this.idUsuario = idUsuario;
    }

    /**
     * Intento de operar sobre los eventos de una productora que el usuario no gestiona: crear uno a
     * su nombre, o listar sus borradores.
     *
     * <p>Tambien es el camino cuando la productora <b>no existe</b>: preguntar si alguien puede
     * gestionar una productora inexistente da {@code false}, y responder 403 en vez de 404 no
     * filtra si ese id existe o no.
     */
    public static ProductoraNoGestionableException alGestionar(Long idProductora, Long idUsuario) {
        return new ProductoraNoGestionableException(
                "El usuario " + idUsuario + " no puede gestionar los eventos de la productora "
                        + idProductora,
                idProductora, idUsuario);
    }

    /** Intento de operar sobre un evento existente que pertenece a otra productora. */
    public static ProductoraNoGestionableException alOperarSobreEvento(
            Long idEvento, Long idProductora, Long idUsuario) {
        return new ProductoraNoGestionableException(
                "El usuario " + idUsuario + " no puede operar sobre el evento " + idEvento
                        + ", que pertenece a la productora " + idProductora,
                idProductora, idUsuario);
    }

    public Long getIdProductora() {
        return idProductora;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }
}
