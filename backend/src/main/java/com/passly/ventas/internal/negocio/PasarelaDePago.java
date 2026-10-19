package com.passly.ventas.internal.negocio;

import com.passly.ventas.PagoRechazadoException;
import com.passly.ventas.dto.DatosDeTarjetaDTO;

import java.math.BigDecimal;

/**
 * <b>Port</b> hacia la pasarela de pago externa (patron Adapter, CLAUDE.md 4.9). Es la interfaz
 * que {@code VentaServiceImpl} conoce; {@link PasarelaDePagoReal} es el adapter de hoy,
 * package-private igual que ella, y delega a {@code PagoService} (PAS-7, PAS-29).
 *
 * <p><b>Por que el Port conoce {@code ventas.dto.DatosDeTarjetaDTO} y no el de {@code pagos}.</b>
 * Ese DTO es, igual que {@link PagoRechazadoException}, parte del contrato propio de este Port
 * -- no del proveedor concreto. Si la firma mencionara {@code pagos.dto.DatosDeTarjetaDTO},
 * cualquier adapter futuro quedaria forzado a conocer la forma de un modulo ajeno aunque no le
 * hablara. Solo {@link PasarelaDePagoReal}, el unico lado que de verdad conoce {@code pagos},
 * traduce un tipo al otro.
 */
interface PasarelaDePago {

    /**
     * Cobra un importe, fuera de cualquier transaccion de base de datos (CLAUDE.md 4.11: un
     * externo lento no puede mantener una fila bloqueada, y esto no es rollbackeable).
     *
     * @param referencia propia de quien pide el cobro (hoy un UUID generado antes de cobrar,
     *                    porque la orden todavia no tiene id) -- permite conciliar el cobro
     *                    contra la compra que lo origino.
     * @throws PagoRechazadoException si la pasarela rechaza el cobro
     */
    ComprobanteDeCobroDTO cobrar(BigDecimal importe, String referencia, DatosDeTarjetaDTO tarjeta);

    /**
     * Revierte un cobro previamente aprobado. Se invoca si, despues de cobrar, la transaccion de
     * confirmar la compra falla — el mismo problema de doble escritura que en la Obligatoria 2
     * resuelve la cola con reintento (CLAUDE.md 2), aca resuelto con una compensacion best-effort:
     * si esta llamada tambien fallara, el cobro queda huerfano y solo hay un log en ERROR. Es una
     * deuda declarada, no un caso cubierto.
     */
    void revertir(String idComprobante);
}
