package com.passly.productoras;

/**
 * El usuario que intenta operar no tiene en esta productora el permiso que la operacion pide.
 *
 * <p>Cubre los dos casos con el mismo status: no ser miembro en absoluto, y ser miembro sin el rol
 * interno suficiente (un VALIDADOR que intenta gestionar el padron, por ejemplo). Se mapea a
 * <b>403 Forbidden</b>, no a 404: el recurso existe y se sabe cual es, lo que falta es la
 * autorizacion.
 *
 * <p><b>No expone el rol requerido en el mensaje</b> a proposito: decirle a un tercero "hace falta
 * ser DUENIO" filtra la estructura interna del padron de una productora ajena.
 */
public class NoEsMiembroDeLaProductoraException extends RuntimeException {

    private final Long idProductora;
    private final Long idUsuario;

    public NoEsMiembroDeLaProductoraException(Long idProductora, Long idUsuario) {
        super("El usuario " + idUsuario + " no esta autorizado a operar sobre la productora "
                + idProductora);
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
