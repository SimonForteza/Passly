package com.passly.pagos.internal.web;

import com.passly.pagos.PagoService;
import com.passly.pagos.dto.ResultadoDeCobroDTO;
import com.passly.pagos.dto.SolicitudDeCobroDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Traduce HTTP a llamadas sobre {@link PagoService}. Sin logica de dominio y sin conocer
 * entidades (CLAUDE.md 4.5).
 *
 * <p><b>Es temporal como puerta de entrada publica.</b> En la arquitectura objetivo,
 * {@code ServicioDeVentas} llama a {@link PagoService} en proceso, dentro de su propio flujo de
 * compra — no por HTTP. Este endpoint existe porque Ventas todavia no existe (CLAUDE.md 4.6) y el
 * criterio de aceptacion de PAS-7 pide poder probar el Adapter con curl. El dia que Ventas exista,
 * este controller puede dejar de ser necesario sin que {@link PagoService} cambie.
 *
 * <p>No lleva {@code @PreAuthorize} de rol: cualquier usuario autenticado puede intentar un
 * cobro, igual que cualquiera puede comprar (COMPRADOR es implicito, CLAUDE.md 4.11). Alcanza con
 * el {@code .anyRequest().authenticated()} por defecto del filter chain.
 *
 * <p>Package-private: nada fuera de este paquete necesita nombrarla, Spring la registra igual
 * como bean {@code @RestController}.
 */
@RestController
class PagoController {

    private final PagoService pagoService;

    PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping("/api/pagos")
    ResponseEntity<ResultadoDeCobroDTO> cobrar(@Valid @RequestBody SolicitudDeCobroDTO solicitud) {
        ResultadoDeCobroDTO resultado = pagoService.cobrar(solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }
}
