package com.passly.ventas.internal.negocio;

import com.passly.ventas.PagoRechazadoException;

import java.math.BigDecimal;

/**
 * <b>Port</b> hacia la pasarela de pago externa (patron Adapter, CLAUDE.md 4.9). Es la interfaz
 * que {@code VentaServiceImpl} conoce; {@link PasarelaDePagoSimulada} es el adapter de hoy,
 * package-private igual que ella.
 *
 * <p><b>Por que existe esta interfaz y no un stub suelto.</b> {@code ServicioDePagos} (PAS-7) es
 * responsabilidad de otro integrante y todavia no existe. Contra este port, el dia que aparezca
 * solo hay que escribir un segundo adapter (p.ej. {@code PasarelaDePagoReal}, delegando a
 * {@code PagoService}) — {@code VentaServiceImpl} no se toca, porque nunca conocio la
 * implementacion concreta.
 */
interface PasarelaDePago {

    /**
     * Cobra un importe, fuera de cualquier transaccion de base de datos (CLAUDE.md 4.11: un
     * externo lento no puede mantener una fila bloqueada, y esto no es rollbackeable).
     *
     * @throws PagoRechazadoException si la pasarela rechaza el cobro
     */
    ComprobanteDeCobroDTO cobrar(BigDecimal importe);

    /**
     * Revierte un cobro previamente aprobado. Se invoca si, despues de cobrar, la transaccion de
     * confirmar la compra falla — el mismo problema de doble escritura que en la Obligatoria 2
     * resuelve la cola con reintento (CLAUDE.md 2), aca resuelto con una compensacion best-effort:
     * si esta llamada tambien fallara, el cobro queda huerfano y solo hay un log en ERROR. Es una
     * deuda declarada, no un caso cubierto.
     */
    void revertir(String idComprobante);
}
