package com.passly.usuarios;

/**
 * No existe un usuario con el identificador pedido.
 *
 * <p>Es publica y vive en la raiz del modulo porque las excepciones de dominio son parte del
 * contrato: quien llame a {@link UsuarioService} tiene que poder distinguir "no existe" de
 * cualquier otro fallo.
 *
 * <p>Extiende {@code RuntimeException} para que {@code @Transactional} revierta la transaccion
 * por defecto, sin necesidad de declarar {@code rollbackFor}.
 */
public class UsuarioNoEncontradoException extends RuntimeException {

    private final Long idUsuario;

    public UsuarioNoEncontradoException(Long idUsuario) {
        super("No existe un usuario con id " + idUsuario);
        this.idUsuario = idUsuario;
    }

    /**
     * Variante para la busqueda por email. {@code idUsuario} queda en {@code null} porque no se
     * busco por id: el identificador que fallo es el email, y va en el mensaje.
     */
    public UsuarioNoEncontradoException(String email) {
        super("No existe un usuario con email " + email);
        this.idUsuario = null;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }
}
