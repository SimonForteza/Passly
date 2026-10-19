package com.passly.ventas.internal.negocio;

import com.passly.pagos.PagoService;
import com.passly.pagos.dto.ResultadoDeCobroDTO;
import com.passly.pagos.dto.SolicitudDeCobroDTO;
import com.passly.ventas.PagoRechazadoException;
import com.passly.ventas.dto.DatosDeTarjetaDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Adapter de {@link PasarelaDePago} que delega a {@code ServicioDePagos} (PAS-7, cableado en
 * PAS-29) en vez de simular el cobro en memoria. Reemplaza a la antigua
 * {@code PasarelaDePagoSimulada} en el camino feliz -- no puede haber dos beans
 * {@link PasarelaDePago} activos, y la simulada se borro porque los tests de integracion ya
 * corren contra el mock HTTP real de {@code pagos} en el mismo proceso.
 *
 * <p><b>El id del comprobante es el {@code idPago}, no el {@code idTransaccionExterna}.</b> Es
 * el identificador propio del modulo {@code pagos}, ya indexado por {@code findById}, asi que
 * {@link #revertir} puede resolverlo directo con {@code Long.valueOf(idComprobante)} sin una
 * consulta nueva por un dato (el id de la pasarela) que es vocabulario ajeno.
 */
@Component
class PasarelaDePagoReal implements PasarelaDePago {

    private static final String MONEDA = "ARS";

    private final PagoService pagoService;

    PasarelaDePagoReal(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @Override
    public ComprobanteDeCobroDTO cobrar(BigDecimal importe, String referencia, DatosDeTarjetaDTO tarjeta) {
        try {
            ResultadoDeCobroDTO resultado = pagoService.cobrar(new SolicitudDeCobroDTO(
                    importe, MONEDA, referencia, aContratoDePagos(tarjeta)));

            return new ComprobanteDeCobroDTO(
                    String.valueOf(resultado.idPago()), resultado.monto(), resultado.fechaProcesamiento());
        } catch (com.passly.pagos.PagoRechazadoException rechazoDePagos) {
            // Traduce la excepcion ajena (pagos) a la propia de este modulo: el nombre es
            // idéntico a proposito (misma semantica), pero son clases distintas con contratos
            // de constructor distintos.
            throw new PagoRechazadoException(rechazoDePagos.getMessage());
        }
    }

    @Override
    public void revertir(String idComprobante) {
        pagoService.revertir(Long.valueOf(idComprobante));
    }

    private com.passly.pagos.dto.DatosDeTarjetaDTO aContratoDePagos(DatosDeTarjetaDTO tarjeta) {
        return new com.passly.pagos.dto.DatosDeTarjetaDTO(
                tarjeta.numero(), tarjeta.titular(), tarjeta.vencimiento(), tarjeta.codigoSeguridad());
    }
}
