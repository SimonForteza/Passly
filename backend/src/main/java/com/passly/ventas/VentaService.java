package com.passly.ventas;

import com.passly.ventas.dto.AgregarItemRequest;
import com.passly.ventas.dto.CarritoDTO;
import com.passly.ventas.dto.OrdenDTO;

import java.util.List;

/**
 * Contrato publico de <b>ServicioDeVentas</b>: el Facade de la compra (CLAUDE.md 4.9).
 *
 * <p>Es la unica puerta de entrada al componente. La implementacion, el carrito
 * {@code @SessionScope}, las entidades JPA y los repositorios viven en {@code internal} y no
 * forman parte de este contrato.
 *
 * <p><b>Sobre {@code idComprador}:</b> viaja como parametro aparte de la solicitud, nunca
 * adentro, igual que {@code idUsuarioActuante} en {@code EventoService}. Sale de
 * {@code Authentication#getName()} en la capa web.
 *
 * <p><b>Estado conversacional, no de negocio.</b> El carrito es estado en memoria administrado
 * por el contenedor (scope de sesion), distinto del estado de negocio que persisten
 * {@link #confirmarCompra(Long)} y el resto de los componentes stateless del sistema
 * (CLAUDE.md 4.7).
 */
public interface VentaService {

    /**
     * Agrega una linea al carrito de quien opera, o lo crea si es la primera. El precio se
     * congela al momento de agregar la linea, contra {@code EventoService.consultarDisponibilidad}.
     *
     * @throws com.passly.eventos.TipoEntradaNoEncontradoException si el tipo de entrada no existe
     */
    CarritoDTO agregarItem(AgregarItemRequest solicitud, Long idComprador);

    /**
     * El carrito de quien opera: sus lineas, el total y cuanto le queda. Devuelve un carrito
     * vacio si todavia no agrego nada, nunca un error.
     *
     * @throws CarritoDeOtroCompradorException si el carrito de esta sesion es de otro comprador
     */
    CarritoDTO verCarrito(Long idComprador);

    /**
     * El Facade: cobra fuera de la transaccion y, si el cobro se aprueba, descuenta cupo y
     * registra la orden dentro de una transaccion declarativa. Si un paso falla, se revierte
     * todo y se compensa el cobro (CLAUDE.md 4.11).
     *
     * @throws CarritoVacioException              si no hay lineas para confirmar
     * @throws CarritoVencidoException             si pasaron mas de ~5 minutos desde que se creo
     * @throws PagoRechazadoException              si la pasarela rechaza el cobro
     * @throws com.passly.eventos.CupoInsuficienteException si el cupo se agoto entre el hold y la confirmacion
     */
    OrdenDTO confirmarCompra(Long idComprador);

    /**
     * Una orden por id, solo si es del comprador que pregunta.
     *
     * @throws OrdenNoEncontradaException si no existe, o si no es de {@code idComprador}
     */
    OrdenDTO consultarOrden(Long idOrden, Long idComprador);

    /** Las ordenes de un comprador, de la mas reciente a la mas antigua. */
    List<OrdenDTO> listarOrdenesDeComprador(Long idComprador);
}
