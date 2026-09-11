package com.passly.productoras.internal;

import com.passly.productoras.ProductoraService;
import com.passly.productoras.RolEnProductora;
import com.passly.productoras.dto.AgregarMiembroRequest;
import com.passly.productoras.dto.CrearProductoraRequest;
import com.passly.productoras.dto.ProductoraDTO;
import com.passly.usuarios.UsuarioService;
import com.passly.usuarios.dto.UsuarioDTO;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Siembra dos productoras de demo, activa solo con el perfil {@code demo}.
 *
 * <p><b>Dos y no una, a proposito.</b> Con una sola productora el sistema es indistinguible del
 * anterior: no se puede mostrar que un comprador elige entre fiestas de distintos organizadores, ni
 * que una productora recibe 403 al tocar el evento de otra. La segunda productora es lo que hace
 * demostrable el marketplace.
 *
 * <p>Corre segundo ({@code @Order(20)}): despues de los usuarios, que necesita para armar el padron,
 * y antes de los eventos, que necesitan productoras a las que pertenecer. Resuelve los usuarios
 * <b>por email</b> y no por id, porque los ids son {@code IDENTITY} y por lo tanto impredecibles.
 *
 * <p>Siembra a traves de {@link ProductoraService}, asi que los datos pasan por las mismas
 * validaciones que produccion — incluido el cruce entre rol global y rol interno. Si el seeder
 * arranca sin excepciones, esas reglas funcionan: el seed es en si mismo un smoke test.
 */
@Component
@Profile("demo")
@Order(20)
class CargaDeProductorasDemo implements ApplicationRunner {

    private final ProductoraService productoraService;
    private final UsuarioService usuarioService;

    CargaDeProductorasDemo(ProductoraService productoraService, UsuarioService usuarioService) {
        this.productoraService = productoraService;
        this.usuarioService = usuarioService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!productoraService.listarProductoras().isEmpty()) {
            return;
        }

        UsuarioDTO omar = usuarioService.consultarUsuarioPorEmail("organizador@passly.test");
        UsuarioDTO olga = usuarioService.consultarUsuarioPorEmail("organizador2@passly.test");
        UsuarioDTO vera = usuarioService.consultarUsuarioPorEmail("validador@passly.test");

        ProductoraDTO aurora = productoraService.crearProductora(new CrearProductoraRequest(
                "Aurora Producciones",
                "30712345678",
                "Productora de festivales al aire libre y ciclos de jazz.",
                null
        ), omar.id());

        // Vera entra como VALIDADOR de Aurora: es miembro, pero NO puede gestionar eventos. Es el
        // caso que separa los dos ejes — pertenecer a una productora no es lo mismo que poder
        // publicar sus fiestas.
        productoraService.agregarMiembro(
                aurora.id(),
                new AgregarMiembroRequest(vera.id(), RolEnProductora.VALIDADOR),
                omar.id());

        productoraService.crearProductora(new CrearProductoraRequest(
                "Nocturna Live",
                "30787654321",
                "Fiestas electronicas y conferencias nocturnas.",
                null
        ), olga.id());
    }
}
