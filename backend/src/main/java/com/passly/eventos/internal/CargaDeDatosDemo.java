package com.passly.eventos.internal;

import com.passly.eventos.EventoService;
import com.passly.eventos.dto.CrearEventoRequest;
import com.passly.eventos.dto.CrearTipoEntradaRequest;
import com.passly.eventos.dto.EventoDTO;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Siembra datos de demo, activa solo con el perfil {@code demo}
 * ({@code -Dspring-boot.run.profiles=demo}). Sin el perfil, la base arranca vacia.
 *
 * <p>Siembra <b>a traves de {@link EventoService}</b> y no con SQL crudo: los datos de prueba
 * pasan por las mismas validaciones que produccion, asi que si el seeder arranca sin excepciones,
 * las reglas de negocio funcionan — el seed es en si mismo un smoke test. Tambien evita el orden
 * entre {@code data.sql} y {@code ddl-auto: update}, y es idempotente: si ya hay eventos
 * publicados, no hace nada.
 */
@Component
@Profile("demo")
class CargaDeDatosDemo implements ApplicationRunner {

    private final EventoService eventoService;

    CargaDeDatosDemo(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!eventoService.listarEventosPublicados().isEmpty()) {
            return;
        }

        EventoDTO jazz = eventoService.crearEvento(new CrearEventoRequest(
                "Festival de Jazz - Verano",
                "Una noche de jazz en vivo al aire libre.",
                OffsetDateTime.now().plusMonths(2),
                "Parque Centenario, CABA",
                List.of(
                        new CrearTipoEntradaRequest("General", new BigDecimal("15000.00"), 200),
                        new CrearTipoEntradaRequest("VIP", new BigDecimal("35000.00"), 50)
                )
        ));
        eventoService.publicarEvento(jazz.id());

        EventoDTO tech = eventoService.crearEvento(new CrearEventoRequest(
                "Conferencia Tech 2027",
                "Charlas sobre arquitectura de software y nuevas tecnologias.",
                OffsetDateTime.now().plusMonths(3),
                "Centro de Convenciones, CABA",
                List.of(new CrearTipoEntradaRequest("General", new BigDecimal("8000.00"), 300))
        ));
        eventoService.publicarEvento(tech.id());

        // Queda en BORRADOR a proposito: la demo necesita algo real para publicar en vivo.
        eventoService.crearEvento(new CrearEventoRequest(
                "Recital sorpresa",
                "Todavia no confirmado.",
                OffsetDateTime.now().plusMonths(1),
                "A confirmar",
                List.of(new CrearTipoEntradaRequest("Early Bird", new BigDecimal("12000.00"), 100))
        ));
    }
}
