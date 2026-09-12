package com.passly.productoras;

/**
 * El usuario ya pertenece al padron de esta productora.
 *
 * <p>Choca con un recurso existente, asi que es <b>409 Conflict</b>, igual que
 * {@link NombreComercialYaRegistradoException}.
 *
 * <p><b>Por que existe y no se deja que falle la base:</b> hay un {@code unique(productora_id,
 * usuario_id)} en la tabla, pero llegar hasta ahi convertiria un caso de uso trivial — invitar dos
 * veces a la misma persona — en un 500 por violacion de integridad. La comprobacion explicita lo
 * devuelve como error de dominio; la constraint queda como red de ultima instancia ante escrituras
 * concurrentes.
 *
 * <p>Cambiarle el rol a un miembro que ya esta en el padron es una operacion distinta, que todavia
 * no existe en el contrato.
 */
public class YaEsMiembroDeLaProductoraException extends RuntimeException {

    private final Long idProductora;
    private final Long idUsuario;

    public YaEsMiembroDeLaProductoraException(Long idProductora, Long idUsuario) {
        super("El usuario " + idUsuario + " ya es miembro de la productora " + idProductora);
        this.idProductora = idProductora;
        this.idUsuario = idUsuario;
    }

    public Long getIdProductora() {
        return idProductora;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }
}
