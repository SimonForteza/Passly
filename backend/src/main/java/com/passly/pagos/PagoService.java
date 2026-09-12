package com.passly.pagos;

import com.passly.pagos.dto.ResultadoDeCobroDTO;
import com.passly.pagos.dto.SolicitudDeCobroDTO;

/**
 * Contrato publico de <b>ServicioDePagos</b>: cobra un monto a traves de la pasarela de pago
 * externa.
 *
 * <p>Es la unica puerta de entrada al componente. La implementacion, el mock de la pasarela y el
 * acceso a datos viven en {@code internal} y no forman parte de este contrato.
 *
 * <p><b>Es un Adapter.</b> {@code PagoService} expone la interfaz de "proveedor externo" que el
 * resto de Passly entiende (una solicitud de cobro, un resultado con estado APROBADO/RECHAZADO);
 * puertas adentro, la implementacion traduce esa forma al contrato propio de la pasarela — que
 * tiene sus propios nombres de campo, como cualquier API de un tercero. Cuando
 * {@code ServicioDeFacturacion} adapte SOAP/AFIP, va a resolver el mismo problema con el mismo
 * patron sobre un protocolo distinto.
 *
 * <p><b>No depende de nadie mas en Passly.</b> Hoy no se persigue: se llama a este servicio
 * directo por HTTP desde afuera (ver {@code PagoController}), a falta de {@code ServicioDeVentas}.
 * Cuando Ventas exista, la va a llamar en proceso, dentro de su propia transaccion de negocio —
 * pero la llamada a la pasarela en si <b>nunca</b> debe quedar envuelta en esa transaccion: es
 * lenta y externa, y CLAUDE.md 2 explica por que eso bloquearia filas sin necesidad.
 */
public interface PagoService {

    /**
     * Cobra un monto a traves de la pasarela de pago externa.
     *
     * @throws PagoRechazadoException             si la pasarela proceso el pedido y rechazo el
     *                                             cobro
     * @throws PasarelaDePagoNoDisponibleException si la pasarela no respondio (caida, timeout)
     */
    ResultadoDeCobroDTO cobrar(SolicitudDeCobroDTO solicitud);
}
