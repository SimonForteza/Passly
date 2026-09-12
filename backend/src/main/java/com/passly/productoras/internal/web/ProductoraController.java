package com.passly.productoras.internal.web;

import com.passly.productoras.ProductoraService;
import com.passly.productoras.dto.AgregarMiembroRequest;
import com.passly.productoras.dto.CrearProductoraRequest;
import com.passly.productoras.dto.MiembroDTO;
import com.passly.productoras.dto.ProductoraDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Traduce HTTP a llamadas sobre {@link ProductoraService}. Sin logica de dominio y sin conocer
 * entidades: solo trata con la interfaz de negocio y los DTOs (CLAUDE.md 4.5).
 *
 * <p><b>Sobre {@code idUsuarioActuante}:</b> es la identidad de quien opera, y viaja aparte de la
 * solicitud (nunca adentro), tal como {@link ProductoraService} lo pide. Sale de
 * {@link Authentication#getName()}: el {@code UserDetails} que arma el modulo {@code seguridad}
 * usa el id numerico como username (no el email), asi que este controller lo obtiene con un tipo
 * de Spring Security, sin depender de Usuarios para resolver email a id.
 *
 * <p><b>Migracion desde {@code X-Usuario-Id} (post-PAS-6).</b> Antes de que existiera Spring
 * Security el actor viajaba en un header, deliberadamente falsificable, para poder probar el
 * modelo de autorizacion (los dos ejes de rol, CLAUDE.md 4.11) antes de que la autenticacion de
 * verdad estuviera lista. Ese momento ya paso: {@code eventos} migro en el merge de PAS-13 con
 * PAS-6, y este modulo se queda atras a proposito hasta ahora para no mezclar dos cambios en un
 * mismo commit. La migracion no cambio ningun DTO ni ninguna firma de {@link ProductoraService}:
 * la identidad siempre viajo como parametro aparte, nunca dentro del request.
 *
 * <p>Package-private: nada fuera de este paquete necesita nombrarla, Spring la registra igual como
 * bean {@code @RestController}.
 */
@RestController
class ProductoraController {

    private final ProductoraService productoraService;

    ProductoraController(ProductoraService productoraService) {
        this.productoraService = productoraService;
    }

    /** Cartelera de productoras: publico, es lo que le permite al comprador elegir. */
    @GetMapping("/api/productoras")
    List<ProductoraDTO> listarProductoras() {
        return productoraService.listarProductoras();
    }

    /**
     * Las productoras del usuario que opera.
     *
     * <p>Va antes de {@code /{id}} en el archivo por claridad, pero no por necesidad: Spring da
     * precedencia al segmento literal {@code mias} sobre la variable de path, sin importar el orden
     * de declaracion.
     */
    @GetMapping("/api/productoras/mias")
    List<ProductoraDTO> listarMisProductoras(Authentication authentication) {
        return productoraService.listarProductorasDeUsuario(idDelActuante(authentication));
    }

    /** Ficha publica de una productora. */
    @GetMapping("/api/productoras/{id}")
    ProductoraDTO consultarProductora(@PathVariable("id") Long id) {
        return productoraService.consultarProductora(id);
    }

    @PostMapping("/api/productoras")
    ResponseEntity<ProductoraDTO> crearProductora(
            Authentication authentication,
            @Valid @RequestBody CrearProductoraRequest solicitud
    ) {
        ProductoraDTO creada = productoraService.crearProductora(solicitud, idDelActuante(authentication));
        return ResponseEntity
                .created(URI.create("/api/productoras/" + creada.id()))
                .body(creada);
    }

    /** El padron. Exige ser miembro: el servicio lo verifica y responde 403 si no. */
    @GetMapping("/api/productoras/{id}/miembros")
    List<MiembroDTO> listarMiembros(
            @PathVariable("id") Long id,
            Authentication authentication
    ) {
        return productoraService.listarMiembros(id, idDelActuante(authentication));
    }

    @PostMapping("/api/productoras/{id}/miembros")
    ResponseEntity<MiembroDTO> agregarMiembro(
            @PathVariable("id") Long id,
            Authentication authentication,
            @Valid @RequestBody AgregarMiembroRequest solicitud
    ) {
        MiembroDTO miembro = productoraService.agregarMiembro(id, solicitud, idDelActuante(authentication));
        return ResponseEntity
                .created(URI.create("/api/productoras/" + id + "/miembros"))
                .body(miembro);
    }

    /**
     * El {@code UserDetails} de {@code seguridad} usa el id numerico como username (ver
     * {@code DetalleDeUsuarioParaAutenticacion}), asi que {@link Authentication#getName()} ya es el
     * id del actuante — sin resolverlo contra Usuarios, que este modulo no puede importar.
     */
    private static Long idDelActuante(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
