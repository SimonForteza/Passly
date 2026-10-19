package com.passly.ventas.internal.web;
import com.passly.ventas.internal.negocio.*;
import com.passly.ventas.dto.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
@RestController
@RequestMapping("/api/ventas/invitados")
class InvitadoController {
    private final CompraInvitadoService compras;
    private final ConsultaInvitado consultas;
    InvitadoController(CompraInvitadoService c,ConsultaInvitado q) { compras=c; consultas=q; }
    record Datos(@NotBlank @Size(max=150) String nombre,@NotBlank @Email @Size(max=254) String email) {}
    record Recuperacion(@NotNull @Positive Long orden,@NotBlank @Email @Size(max=254) String email) {}
    @GetMapping("/carrito") CarritoDTO ver() { return compras.ver(); }
    @PostMapping("/carrito/items") CarritoDTO agregar(@Valid @RequestBody AgregarItemRequest r) { return compras.agregar(r); }
    @DeleteMapping("/carrito") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    void vaciar() { compras.vaciar(); }
    @PostMapping("/transferencia") CarritoDTO transferir(Authentication a) { return compras.transferir(Long.valueOf(a.getName())); }
    @PostMapping("/ordenes") CompraInvitadoService.Confirmada confirmar(@RequestHeader("Idempotency-Key") String clave,@Valid @RequestBody Datos r) {
        return compras.confirmar(clave,r.nombre(),r.email());
    }
    @GetMapping("/orden") ConsultaInvitado.Detalle consultar(@RequestHeader("X-Orden-Token") String token) { return consultas.consultar(token); }
    @PostMapping("/recuperacion") @ResponseStatus(org.springframework.http.HttpStatus.ACCEPTED)
    void recuperar(@Valid @RequestBody Recuperacion r,HttpServletRequest request) { consultas.recuperar(r.orden(),r.email(),request.getRemoteAddr()); }
}
