package com.passly.pagos.internal.negocio;

import com.passly.pagos.EstadoDePago;
import com.passly.pagos.PagoRechazadoException;
import com.passly.pagos.PagoService;
import com.passly.pagos.PasarelaDePagoNoDisponibleException;
import com.passly.pagos.dto.ResultadoDeCobroDTO;
import com.passly.pagos.dto.SolicitudDeCobroDTO;
import com.passly.pagos.internal.RespuestaExternaDeCobro;
import com.passly.pagos.internal.SolicitudExternaDeCobro;
import com.passly.pagos.internal.datos.Pago;
import com.passly.pagos.internal.datos.PagoRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Implementacion de {@link PagoService}: el Adapter propiamente dicho.
 *
 * <p><b>Package-private a proposito.</b> Spring la instancia igual por reflexion; los demas
 * componentes solo pueden inyectar {@link PagoService} (CLAUDE.md 4.3, regla 1).
 *
 * <p><b>Deliberadamente sin {@code @Transactional}.</b> El unico trabajo con la base es un
 * {@code save()} despues de que la llamada HTTP ya termino, y {@code SimpleJpaRepository.save()}
 * abre su propia transaccion corta para eso. Envolver todo el metodo en
 * {@code @Transactional} dejaria una conexion (y potencialmente una fila) tomada mientras se
 * espera una respuesta de red lenta — exactamente lo que CLAUDE.md 2 explica que hay que evitar
 * con AFIP, aplicado aca a la pasarela de pago.
 */
@Service
class PagoServiceImpl implements PagoService {

    private static final String APROBADO_EXTERNO = "APPROVED";

    private final RestClient pasarelaDePago;
    private final PagoRepository pagoRepository;
    private final PagoMapper mapper;

    PagoServiceImpl(RestClient pasarelaDePago, PagoRepository pagoRepository, PagoMapper mapper) {
        this.pasarelaDePago = pasarelaDePago;
        this.pagoRepository = pagoRepository;
        this.mapper = mapper;
    }

    @Override
    public ResultadoDeCobroDTO cobrar(SolicitudDeCobroDTO solicitud) {
        RespuestaExternaDeCobro respuesta = llamarPasarela(aContratoExterno(solicitud));

        EstadoDePago estado = APROBADO_EXTERNO.equals(respuesta.status())
                ? EstadoDePago.APROBADO
                : EstadoDePago.RECHAZADO;

        // Se persiste el intento haya sido aprobado o rechazado: permite conciliar despues contra
        // el resumen de la pasarela, y deja evidencia de que hubo una llamada real, no una
        // respuesta enlatada. La llamada HTTP ya termino aca, asi que este guardado corre en su
        // propia transaccion corta, sin haber sostenido nada mientras se esperaba la red.
        Pago pago = pagoRepository.save(new Pago(
                solicitud.monto(),
                solicitud.moneda(),
                solicitud.referencia(),
                respuesta.transactionId(),
                estado,
                respuesta.processedAt()
        ));

        if (estado == EstadoDePago.RECHAZADO) {
            throw new PagoRechazadoException(solicitud.referencia(), respuesta.transactionId());
        }

        return mapper.aDTO(pago);
    }

    /**
     * Llama a la pasarela externa y distingue el rechazo de negocio (que llega como una respuesta
     * 200 valida con {@code status = "DECLINED"}, y se resuelve en {@link #cobrar}) de la caida
     * tecnica del proveedor (5xx, timeout, conexion rechazada), que se traduce aca mismo a
     * {@link PasarelaDePagoNoDisponibleException}.
     */
    private RespuestaExternaDeCobro llamarPasarela(SolicitudExternaDeCobro solicitudExterna) {
        try {
            return pasarelaDePago.post()
                    .uri("/mock/pasarela-pago/cobros")
                    .body(solicitudExterna)
                    .retrieve()
                    .body(RespuestaExternaDeCobro.class);
        } catch (HttpServerErrorException | ResourceAccessException fallaDeLaPasarela) {
            throw new PasarelaDePagoNoDisponibleException(
                    solicitudExterna.merchantReference(), fallaDeLaPasarela);
        }
    }

    /**
     * Traduce nuestro contrato al de la pasarela externa. Es la esencia del Adapter: el resto del
     * sistema conoce {@link SolicitudDeCobroDTO}, nunca el vocabulario propio del proveedor.
     */
    private SolicitudExternaDeCobro aContratoExterno(SolicitudDeCobroDTO solicitud) {
        return new SolicitudExternaDeCobro(
                solicitud.monto(),
                solicitud.moneda(),
                solicitud.tarjeta().numero(),
                solicitud.tarjeta().titular(),
                solicitud.tarjeta().vencimiento(),
                solicitud.tarjeta().codigoSeguridad(),
                solicitud.referencia()
        );
    }
}
