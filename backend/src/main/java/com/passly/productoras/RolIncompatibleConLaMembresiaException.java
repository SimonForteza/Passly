package com.passly.productoras;

/**
 * El rol global del usuario no es compatible con el rol interno con el que se lo quiere incorporar.
 *
 * <p>Los dos ejes tienen que cerrar: DUENIO y STAFF exigen que el usuario sea {@code ORGANIZADOR}
 * en el sistema, y VALIDADOR exige que sea {@code VALIDADOR}. Un COMPRADOR no puede entrar al
 * padron de ninguna productora con ningun rol.
 *
 * <p>Es <b>409 Conflict</b> y no 403: no es que a quien pide le falte permiso — la peticion en si es
 * inconsistente con el estado del usuario invitado.
 *
 * <p><b>No expone el enum {@code Rol} de Usuarios</b> en su API. Si lo hiciera, todo consumidor del
 * contrato de Productoras quedaria atado tambien al contrato de Usuarios solo para poder leer esta
 * excepcion. El rol requerido va en el mensaje, como texto.
 */
public class RolIncompatibleConLaMembresiaException extends RuntimeException {

    private final Long idUsuario;
    private final RolEnProductora rolEnProductora;

    public RolIncompatibleConLaMembresiaException(
            Long idUsuario,
            RolEnProductora rolEnProductora,
            String rolGlobalRequerido
    ) {
        super("El usuario " + idUsuario + " no puede incorporarse como " + rolEnProductora
                + " porque su rol en el sistema no es " + rolGlobalRequerido);
        this.idUsuario = idUsuario;
        this.rolEnProductora = rolEnProductora;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public RolEnProductora getRolEnProductora() {
        return rolEnProductora;
    }
}
