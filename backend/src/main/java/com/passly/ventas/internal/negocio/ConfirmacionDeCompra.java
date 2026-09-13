package com.passly.ventas.internal.negocio;

import com.passly.eventos.EventoService;
import com.passly.eventos.dto.DescontarCupoRequest;
import com.passly.ventas.dto.ItemDeCarritoDTO;
import com.passly.ventas.internal.datos.ItemOrden;
import com.passly.ventas.internal.datos.Orden;
import com.passly.ventas.internal.datos.OrdenRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * El limite transaccional real de "confirmar compra" (CLAUDE.md 4.11): descuento de cupo ->
 * registro de la orden, en una sola transaccion declarativa. Si algo falla, se revierte todo.
 *
 * <p><b>Por que este metodo vive en un bean aparte de {@code VentaServiceImpl}, y no es un
 * detalle de estilo.</b> {@code VentaServiceImpl.confirmarCompra} tiene que cobrar <i>antes</i>
 * de esto (CLAUDE.md 2: un externo lento no puede mantener una fila bloqueada, y el cobro no es
 * rollbackeable). Si esa transaccion viviera en el mismo objeto que hace el cobro,
 * {@code @Transactional} tendria que estar en la clase o en el metodo que hace las dos cosas —
 * y como Spring aplica {@code @Transactional} con un proxy, una llamada interna
 * ({@code this.confirmar(...)} desde el mismo objeto) <b>no pasa por el proxy y la anotacion no
 * se aplica</b>. La unica forma de que el limite transaccional sea real es que sea un salto
 * entre beans: {@code VentaServiceImpl} (sin {@code @Transactional}) llama a este bean (que si
 * lo tiene) despues de cobrar, nunca antes.
 *
 * <p><b>El payoff del ADR "monolito modular" (CLAUDE.md 4.2), demostrado y no solo afirmado:</b>
 * {@code EventoService.descontarCupo} ya es {@code @Transactional} en
 * {@code EventoServiceImpl}. Al llamarlo desde aca con la propagacion por defecto
 * ({@code REQUIRED}), se <b>une</b> a esta misma transaccion — un solo commit para el
 * {@code UPDATE} sobre {@code eventos.tipo_entrada} y los {@code INSERT} sobre
 * {@code ventas.orden}/{@code ventas.item_orden}, en dos esquemas de la misma base fisica. Con
 * una base por componente esto seria una saga con compensacion.
 */
@Component
class ConfirmacionDeCompra {

    private final EventoService eventoService;
    private final OrdenRepository ordenRepository;

    ConfirmacionDeCompra(EventoService eventoService, OrdenRepository ordenRepository) {
        this.eventoService = eventoService;
        this.ordenRepository = ordenRepository;
    }

    @Transactional
    Orden confirmar(
            Long idComprador,
            List<ItemDeCarritoDTO> items,
            BigDecimal total,
            String comprobanteCobro,
            OffsetDateTime cobradaEn
    ) {
        eventoService.descontarCupo(items.stream()
                .map(item -> new DescontarCupoRequest(item.idTipoEntrada(), item.cantidad()))
                .toList());

        Orden orden = new Orden(idComprador, total, comprobanteCobro, cobradaEn);
        for (ItemDeCarritoDTO item : items) {
            orden.agregarItem(new ItemOrden(
                    item.idTipoEntrada(), item.idEvento(), item.nombreTipoEntrada(),
                    item.precioUnitario(), item.cantidad()));
        }
        return ordenRepository.save(orden);
    }
}
