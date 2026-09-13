package com.passly.ventas.internal.web;

import com.passly.ventas.VentaService;
import com.passly.ventas.dto.AgregarItemRequest;
import com.passly.ventas.dto.CarritoDTO;
import com.passly.ventas.dto.OrdenDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Traduce HTTP a llamadas sobre {@link VentaService}. Sin logica de dominio y sin conocer
 * entidades: solo trata con la interfaz de negocio y los DTOs (CLAUDE.md 4.5).
 *
 * <p><b>Sobre {@code idDelActuante}:</b> mismo patron que {@code EventoController} — el
 * {@code UserDetails} de {@code seguridad} usa el id numerico como username, asi que
 * {@link Authentication#getName()} ya es el id, sin resolverlo contra Usuarios.
 *
 * <p><b>{@code COMPRADOR} es implicito (CLAUDE.md 4.11):</b> cualquier autenticado compra, y eso
 * ya lo garantiza {@code anyRequest().authenticated()} en el filter chain — por eso ninguna
 * operacion de acá lleva {@code @PreAuthorize} de rol. La unica excepcion,
 * {@link #listarOrdenesDeComprador}, no autoriza por rol sino por <b>identidad</b>: que el
 * comprador solo vea las suyas, salvo que sea {@code ADMIN}.
 *
 * <p>Package-private: nada fuera de este paquete necesita nombrarla.
 */
@RestController
class VentaController {

    private final VentaService ventaService;

    VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    @PostMapping("/api/ventas/carrito/items")
    CarritoDTO agregarItem(
            Authentication authentication,
            @Valid @RequestBody AgregarItemRequest solicitud
    ) {
        return ventaService.agregarItem(solicitud, idDelActuante(authentication));
    }

    @GetMapping("/api/ventas/carrito")
    CarritoDTO verCarrito(Authentication authentication) {
        return ventaService.verCarrito(idDelActuante(authentication));
    }

    /**
     * Abandonar la compra: invalida la sesion para que el contenedor dispare
     * {@code @PreDestroy} del carrito en el acto, en vez de esperar el timeout de sesion
     * (CLAUDE.md 4.7). El DTO de respuesta se arma <b>antes</b> de invalidar: tocar el carrito
     * despues de {@code invalidate()} le pediria a Spring una sesion nueva.
     */
    @DeleteMapping("/api/ventas/carrito")
    CarritoDTO abandonarCarrito(Authentication authentication, HttpServletRequest request) {
        CarritoDTO carrito = ventaService.verCarrito(idDelActuante(authentication));
        request.getSession().invalidate();
        return carrito;
    }

    @PostMapping("/api/ventas/ordenes")
    ResponseEntity<OrdenDTO> confirmarCompra(Authentication authentication) {
        OrdenDTO orden = ventaService.confirmarCompra(idDelActuante(authentication));
        return ResponseEntity.created(URI.create("/api/ventas/ordenes/" + orden.id())).body(orden);
    }

    @GetMapping("/api/ventas/ordenes/{id}")
    OrdenDTO consultarOrden(@PathVariable("id") Long id, Authentication authentication) {
        return ventaService.consultarOrden(id, idDelActuante(authentication));
    }

    /**
     * Rol <b>mas</b> identidad, distinto de los dos {@code @PreAuthorize} que ya existen
     * (CLAUDE.md 4.11): ni pura autorizacion por rol (Eventos, Usuarios) ni pura membresia
     * (Eventos vs Productoras). Un {@code ADMIN} ve las ordenes de cualquiera; cualquier otro
     * autenticado, solo las propias.
     */
    @GetMapping("/api/ventas/compradores/{idComprador}/ordenes")
    @PreAuthorize("hasRole('ADMIN') or #idComprador.equals(T(java.lang.Long).valueOf(authentication.name))")
    List<OrdenDTO> listarOrdenesDeComprador(@PathVariable("idComprador") Long idComprador) {
        return ventaService.listarOrdenesDeComprador(idComprador);
    }

    /**
     * El {@code UserDetails} de {@code seguridad} usa el id numerico como username, asi que
     * {@link Authentication#getName()} ya es el id del actuante.
     */
    private static Long idDelActuante(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
