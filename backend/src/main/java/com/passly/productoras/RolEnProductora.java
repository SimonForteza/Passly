package com.passly.productoras;

/**
 * Rol de un usuario <b>dentro de una productora</b>.
 *
 * <p>No reemplaza a {@code usuarios.Rol}: son dos ejes distintos y ortogonales. {@code Rol} es una
 * capacidad global — que alguien organice, valide o compre. {@code RolEnProductora} es
 * <b>alcance</b>: sobre las fiestas de quien, y con que permisos. Colapsarlos en uno daria o un rol
 * global por productora (perdiendo el sentido del rol) o un unico rol global, y entonces cualquier
 * validador podria escanear los tickets de cualquier productora.
 *
 * <p>La membresia y el rol global se validan cruzados <b>una sola vez</b>, al dar de alta al
 * miembro: DUENIO y STAFF exigen {@code Rol.ORGANIZADOR}, VALIDADOR exige {@code Rol.VALIDADOR}.
 * Despues ninguna operacion vuelve a consultar el rol global.
 *
 * <p>Vive en la raiz del modulo porque es parte del contrato. Las entidades lo mapean con
 * {@code @Enumerated(EnumType.STRING)}, nunca {@code ORDINAL}, para que agregar un rol nuevo no
 * corrompa las filas ya guardadas.
 */
public enum RolEnProductora {

    /** Creo la productora o fue promovido. Es el unico que puede incorporar miembros. */
    DUENIO,

    /** Gestiona los eventos de la productora, pero no su padron de miembros. */
    STAFF,

    /** Escanea los QR de los eventos de esta productora. No gestiona eventos. */
    VALIDADOR;

    /**
     * Si este rol habilita crear, editar y publicar los eventos de la productora.
     *
     * <p>La capacidad vive en el enum y no en un {@code switch} del servicio: es la misma razon por
     * la que {@code Evento.publicar()} vive en la entidad. Si manana aparece un rol nuevo, el lugar
     * donde decidir que habilita es este, y no hay que ir a buscar condicionales desparramados.
     */
    public boolean puedeGestionarEventos() {
        return this == DUENIO || this == STAFF;
    }

    /** Si este rol habilita incorporar o quitar miembros del padron. */
    public boolean puedeGestionarMiembros() {
        return this == DUENIO;
    }
}
