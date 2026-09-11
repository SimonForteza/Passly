package com.passly.demo;

import com.passly.eventos.EventoService;
import com.passly.eventos.dto.CrearEventoRequest;
import com.passly.eventos.dto.CrearTipoEntradaRequest;
import com.passly.eventos.dto.EventoDTO;
import com.passly.productoras.ProductoraService;
import com.passly.productoras.RolEnProductora;
import com.passly.productoras.dto.AgregarMiembroRequest;
import com.passly.productoras.dto.CrearProductoraRequest;
import com.passly.productoras.dto.ProductoraDTO;
import com.passly.usuarios.Rol;
import com.passly.usuarios.UsuarioService;
import com.passly.usuarios.dto.CrearUsuarioRequest;
import com.passly.usuarios.dto.UsuarioDTO;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Siembra el escenario completo de demostracion: usuarios, dos productoras y sus fiestas.
 *
 * <p>Siembra <b>a traves de los servicios</b> y no con SQL crudo: los datos pasan por las mismas
 * validaciones que produccion — el hashing de credenciales, el cruce entre rol global y rol interno,
 * las reglas de publicacion. Si el seeder arranca sin excepciones, esas reglas funcionan: el seed es
 * en si mismo un smoke test. Es idempotente: si ya hay usuarios, no hace nada.
 *
 * <p><b>El escenario esta armado para que la demo se pueda mostrar en vivo</b>, no solo para llenar
 * tablas:
 * <ul>
 *   <li>dos productoras, porque con una sola no hay marketplace que demostrar;</li>
 *   <li>Vera es VALIDADOR de Aurora: es miembro, pero recibe 403 al intentar publicar — es el caso
 *       que separa pertenecer de poder gestionar;</li>
 *   <li>"Recital sorpresa" queda en BORRADOR, para tener algo real que publicar frente al jurado.</li>
 * </ul>
 */
@Component
@Profile("demo")
class CargaDeDatosDemo implements ApplicationRunner {

    /** Password comun para todos los usuarios de demo. Cumple el minimo de 8 caracteres. */
    private static final String PASSWORD_DEMO = "passly1234";

    private final UsuarioService usuarioService;
    private final ProductoraService productoraService;
    private final EventoService eventoService;

    CargaDeDatosDemo(
            UsuarioService usuarioService,
            ProductoraService productoraService,
            EventoService eventoService
    ) {
        this.usuarioService = usuarioService;
        this.productoraService = productoraService;
        this.eventoService = eventoService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!usuarioService.listarUsuarios().isEmpty()) {
            return;
        }

        // ---- 1. Usuarios: uno por rol, mas un segundo organizador para la otra productora ----
        usuarioService.registrarUsuario(new CrearUsuarioRequest(
                "comprador@passly.test", "Carla Compradora", PASSWORD_DEMO, Rol.COMPRADOR));
        usuarioService.registrarUsuario(new CrearUsuarioRequest(
                "admin@passly.test", "Ada Admin", PASSWORD_DEMO, Rol.ADMIN));

        UsuarioDTO omar = usuarioService.registrarUsuario(new CrearUsuarioRequest(
                "organizador@passly.test", "Omar Organizador", PASSWORD_DEMO, Rol.ORGANIZADOR));
        UsuarioDTO olga = usuarioService.registrarUsuario(new CrearUsuarioRequest(
                "organizador2@passly.test", "Olga Organizadora", PASSWORD_DEMO, Rol.ORGANIZADOR));
        UsuarioDTO vera = usuarioService.registrarUsuario(new CrearUsuarioRequest(
                "validador@passly.test", "Vera Validadora", PASSWORD_DEMO, Rol.VALIDADOR));

        // ---- 2. Productoras ----
        ProductoraDTO aurora = productoraService.crearProductora(new CrearProductoraRequest(
                "Aurora Producciones",
                "30712345678",
                "Productora de festivales al aire libre y ciclos de jazz.",
                null
        ), omar.id());

        // Vera es miembro de Aurora pero NO puede gestionar sus eventos: separa los dos ejes.
        productoraService.agregarMiembro(
                aurora.id(),
                new AgregarMiembroRequest(vera.id(), RolEnProductora.VALIDADOR),
                omar.id());

        ProductoraDTO nocturna = productoraService.crearProductora(new CrearProductoraRequest(
                "Nocturna Live",
                "30787654321",
                "Fiestas electronicas y conferencias nocturnas.",
                null
        ), olga.id());

        // ---- 3. Eventos, repartidos entre las dos productoras ----
        EventoDTO jazz = eventoService.crearEvento(new CrearEventoRequest(
                aurora.id(),
                "Festival de Jazz - Verano",
                "Una noche de jazz en vivo al aire libre.",
                OffsetDateTime.now().plusMonths(2),
                "Parque Centenario, CABA",
                List.of(
                        new CrearTipoEntradaRequest("General", new BigDecimal("15000.00"), 200),
                        new CrearTipoEntradaRequest("VIP", new BigDecimal("35000.00"), 50)
                )
        ), omar.id());
        eventoService.publicarEvento(jazz.id(), omar.id());

        EventoDTO tech = eventoService.crearEvento(new CrearEventoRequest(
                nocturna.id(),
                "Conferencia Tech 2027",
                "Charlas sobre arquitectura de software y nuevas tecnologias.",
                OffsetDateTime.now().plusMonths(3),
                "Centro de Convenciones, CABA",
                List.of(new CrearTipoEntradaRequest("General", new BigDecimal("8000.00"), 300))
        ), olga.id());
        eventoService.publicarEvento(tech.id(), olga.id());

        // Queda en BORRADOR a proposito: la demo necesita algo real para publicar en vivo.
        eventoService.crearEvento(new CrearEventoRequest(
                aurora.id(),
                "Recital sorpresa",
                "Todavia no confirmado.",
                OffsetDateTime.now().plusMonths(1),
                "A confirmar",
                List.of(new CrearTipoEntradaRequest("Early Bird", new BigDecimal("12000.00"), 100))
        ), omar.id());
    }
}
