package com.passly.usuarios;

/**
 * Ya existe una cuenta con el email pedido.
 *
 * <p>El recurso no falta: choca con uno que ya existe. Por eso se mapea a <b>409 Conflict</b>,
 * no a un 400 — el mismo relato del 409 que en {@code eventos} al republicar. El guard real de
 * unicidad es el {@code unique} sobre {@code email} en la tabla; esta excepcion es la version de
 * dominio que hace la comprobacion explicita antes de intentar el insert.
 *
 * <p>Es publica y vive en la raiz del modulo porque es parte del contrato. Extiende
 * {@code RuntimeException} para que {@code @Transactional} revierta por defecto.
 */
public class EmailYaRegistradoException extends RuntimeException {

    private final String email;

    public EmailYaRegistradoException(String email) {
        super("Ya existe un usuario registrado con el email " + email);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
