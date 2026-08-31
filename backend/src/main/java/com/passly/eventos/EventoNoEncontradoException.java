package com.passly.eventos;

/**
 * No existe un evento con el identificador pedido.
 *
 * <p>Es publica y vive en la raiz del modulo porque las excepciones de dominio son parte del
 * contrato: quien llame a {@link EventoService} — hoy la app web, manana
 * {@code ServicioDeVentas} — tiene que poder distinguir "no existe" de cualquier otro fallo.
 *
 * <p>Extiende {@code RuntimeException} para que {@code @Transactional} revierta la transaccion
 * por defecto, sin necesidad de declarar {@code rollbackFor}.
 */
public class EventoNoEncontradoException extends RuntimeException {

    private final Long idEvento;

    public EventoNoEncontradoException(Long idEvento) {
        super("No existe un evento con id " + idEvento);
        this.idEvento = idEvento;
    }

    public Long getIdEvento() {
        return idEvento;
    }
}
