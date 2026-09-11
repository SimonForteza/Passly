package com.passly.productoras;

import com.passly.productoras.dto.AgregarMiembroRequest;
import com.passly.productoras.dto.CrearProductoraRequest;
import com.passly.productoras.dto.MiembroDTO;
import com.passly.productoras.dto.ProductoraDTO;

import java.util.List;

/**
 * Contrato publico de <b>ServicioDeProductoras</b>: las identidades comerciales que publican fiestas
 * y el padron de quienes pueden operar en nombre de cada una.
 *
 * <p>Es la unica puerta de entrada al componente. La implementacion, las entidades y el repositorio
 * viven en {@code internal} y no forman parte de este contrato.
 *
 * <p><b>Posicion en el sistema:</b> Productoras depende de Usuarios (valida identidad y rol global)
 * y es consumido por Eventos (que le pregunta quien puede gestionar los eventos de una productora).
 * Es el eslabon del medio de la cadena {@code eventos -> productoras -> usuarios} que
 * {@code EstructuraDeModulosTest} verifica en cada build. Es <b>stateless</b>: no guarda estado
 * conversacional entre llamadas.
 *
 * <p><b>Sobre las tres consultas booleanas del final:</b> son preguntas de negocio, no getters del
 * padron. Eventos pregunta {@link #puedeGestionarEventos(Long, Long)} y Productoras decide
 * internamente que roles califican; si manana aparece un rol nuevo, Eventos no se toca y ni siquiera
 * necesita conocer {@link RolEnProductora}. Es <i>tell, don't ask</i>: la alternativa — exponer el
 * padron y que cada consumidor evalue el rol por su cuenta — desparramaria la misma regla por todo
 * el sistema.
 */
public interface ProductoraService {

    /**
     * Da de alta una productora. Quien la crea queda como su primer miembro, con rol
     * {@link RolEnProductora#DUENIO}.
     *
     * <p>Si el usuario creador no existe, se propaga la excepcion de "usuario no encontrado" de
     * Usuarios, que ya resuelve a 404. No se la traduce a un tipo propio: seria una capa de
     * indireccion que no agrega informacion.
     *
     * @param idUsuarioCreador identidad de quien opera; tiene que ser un ORGANIZADOR
     * @throws NombreComercialYaRegistradoException   si el nombre comercial ya esta tomado
     * @throws RolIncompatibleConLaMembresiaException si el creador no es ORGANIZADOR
     */
    ProductoraDTO crearProductora(CrearProductoraRequest solicitud, Long idUsuarioCreador);

    /**
     * Devuelve una productora por id.
     *
     * @throws ProductoraNoEncontradaException si no existe
     */
    ProductoraDTO consultarProductora(Long idProductora);

    /** Lista todas las productoras, ordenadas por nombre comercial ascendente. */
    List<ProductoraDTO> listarProductoras();

    /** Las productoras de las que el usuario es miembro, con cualquier rol interno. */
    List<ProductoraDTO> listarProductorasDeUsuario(Long idUsuario);

    /**
     * Incorpora un usuario al padron de la productora.
     *
     * <p>Valida los dos ejes cruzados: que quien pide sea DUENIO, y que el rol global del invitado
     * sea compatible con el rol interno pedido. Es el <b>unico</b> lugar del sistema donde se
     * consulta el rol global para decidir una membresia; despues, ninguna operacion vuelve a
     * consultarlo.
     *
     * @param idUsuarioSolicitante identidad de quien opera; tiene que ser DUENIO
     * @throws ProductoraNoEncontradaException        si la productora no existe
     * @throws NoEsMiembroDeLaProductoraException     si el solicitante no es DUENIO
     * @throws YaEsMiembroDeLaProductoraException     si el invitado ya esta en el padron
     * @throws RolIncompatibleConLaMembresiaException si el rol global del invitado no corresponde
     */
    MiembroDTO agregarMiembro(
            Long idProductora,
            AgregarMiembroRequest solicitud,
            Long idUsuarioSolicitante);

    /**
     * El padron de la productora, con el email y el nombre de cada miembro resueltos contra
     * Usuarios.
     *
     * @throws ProductoraNoEncontradaException si no existe
     */
    List<MiembroDTO> listarMiembros(Long idProductora);

    /**
     * Si el usuario pertenece al padron de la productora, con cualquier rol interno.
     *
     * <p>Devuelve {@code false} — no lanza — si la productora no existe: es una consulta, y para
     * quien pregunta "no pertenece" y "no existe" tienen la misma consecuencia.
     */
    boolean esMiembro(Long idProductora, Long idUsuario);

    /**
     * Si el usuario puede crear, editar y publicar los eventos de esta productora.
     *
     * <p>Es la pregunta que hace {@code ServicioDeEventos} antes de aceptar cualquier operacion de
     * gestion. Hoy responde {@code true} para DUENIO y STAFF; un VALIDADOR, un no-miembro o una
     * productora inexistente dan {@code false}.
     */
    boolean puedeGestionarEventos(Long idProductora, Long idUsuario);
}
