package com.passly.productoras.internal.web;

import com.passly.productoras.ProductoraService;
import com.passly.productoras.dto.AgregarMiembroRequest;
import com.passly.productoras.dto.CrearProductoraRequest;
import com.passly.productoras.dto.MiembroDTO;
import com.passly.productoras.dto.ProductoraDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Traduce HTTP a llamadas sobre {@link ProductoraService}. Sin logica de dominio y sin conocer
 * entidades: solo trata con la interfaz de negocio y los DTOs (CLAUDE.md 4.5).
 *
 * <p><b>Sobre el header {@code X-Usuario-Id}:</b> es la identidad de quien opera, y es
 * <b>temporal</b>. Spring Security todavia no existe en el proyecto (es PAS-6), asi que hasta
 * entonces el actor viaja en un header. Es deliberadamente falsificable y no pretende ser
 * seguridad: lo que si logra es que <b>el modelo de autorizacion ya este completo y probado</b>
 * cuando llegue la autenticacion de verdad.
 *
 * <p>La migracion es una linea por endpoint: {@code @RequestHeader("X-Usuario-Id") Long} pasa a
 * {@code @AuthenticationPrincipal}. No cambia ningun DTO ni ninguna firma de
 * {@link ProductoraService}, justamente porque la identidad viaja como parametro aparte y nunca
 * dentro del request.
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
    List<ProductoraDTO> listarMisProductoras(
            @RequestHeader("X-Usuario-Id") Long idUsuarioActuante
    ) {
        return productoraService.listarProductorasDeUsuario(idUsuarioActuante);
    }

    /** Ficha publica de una productora. */
    @GetMapping("/api/productoras/{id}")
    ProductoraDTO consultarProductora(@PathVariable("id") Long id) {
        return productoraService.consultarProductora(id);
    }

    @PostMapping("/api/productoras")
    ResponseEntity<ProductoraDTO> crearProductora(
            @RequestHeader("X-Usuario-Id") Long idUsuarioActuante,
            @Valid @RequestBody CrearProductoraRequest solicitud
    ) {
        ProductoraDTO creada = productoraService.crearProductora(solicitud, idUsuarioActuante);
        return ResponseEntity
                .created(URI.create("/api/productoras/" + creada.id()))
                .body(creada);
    }

    /** El padron. Exige ser miembro: el servicio lo verifica y responde 403 si no. */
    @GetMapping("/api/productoras/{id}/miembros")
    List<MiembroDTO> listarMiembros(
            @PathVariable("id") Long id,
            @RequestHeader("X-Usuario-Id") Long idUsuarioActuante
    ) {
        return productoraService.listarMiembros(id, idUsuarioActuante);
    }

    @PostMapping("/api/productoras/{id}/miembros")
    ResponseEntity<MiembroDTO> agregarMiembro(
            @PathVariable("id") Long id,
            @RequestHeader("X-Usuario-Id") Long idUsuarioActuante,
            @Valid @RequestBody AgregarMiembroRequest solicitud
    ) {
        MiembroDTO miembro = productoraService.agregarMiembro(id, solicitud, idUsuarioActuante);
        return ResponseEntity
                .created(URI.create("/api/productoras/" + id + "/miembros"))
                .body(miembro);
    }
}
