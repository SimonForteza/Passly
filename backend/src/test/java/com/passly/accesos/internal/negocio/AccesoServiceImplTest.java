package com.passly.accesos.internal.negocio;

import com.passly.accesos.dto.ValidacionDTO;
import com.passly.accesos.dto.ValidacionDTO.Resultado;
import com.passly.accesos.dto.ValidarEntradaRequest;
import com.passly.accesos.internal.datos.Validacion;
import com.passly.accesos.internal.datos.ValidacionRepository;
import com.passly.eventos.EstadoEvento;
import com.passly.eventos.EventoService;
import com.passly.eventos.dto.EventoDTO;
import com.passly.eventos.dto.OrganizadorDeEventoDTO;
import com.passly.productoras.NoEsMiembroDeLaProductoraException;
import com.passly.productoras.ProductoraService;
import com.passly.tickets.TicketService;
import com.passly.tickets.dto.UsoTicketDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unitario puro de la validacion en puerta (PAS-38). Tickets queda mockeado: Accesos no decide
 * si un QR es valido (eso es custodia de Tickets, PAS-31), decide quien puede validar, en que
 * evento, y que se audita y se le muestra al validador.
 */
class AccesoServiceImplTest {

    private static final Long EVENTO = 5L;
    private static final Long PRODUCTORA = 1L;
    private static final Long VALIDADOR = 12L;
    private static final String QR = "contenido-firmado";

    private final EventoService eventos = mock(EventoService.class);
    private final ProductoraService productoras = mock(ProductoraService.class);
    private final TicketService tickets = mock(TicketService.class);
    private final ValidacionRepository validaciones = mock(ValidacionRepository.class);
    private final AccesoServiceImpl servicio = new AccesoServiceImpl(eventos, productoras, tickets, validaciones);

    @ParameterizedTest(name = "{0} -> ticket informado={1}")
    @CsvSource({
            "VALIDO,      true,  Entrada valida. Puede ingresar.",
            "YA_USADO,    true,  Esta entrada ya fue utilizada.",
            "OTRO_EVENTO, false, Esta entrada pertenece a otro evento.",
            "INVALIDO,    false, El QR no corresponde a una entrada valida."
    })
    void traduceCadaResultadoDeTicketsYLoAudita(Resultado resultado, boolean conTicket, String mensaje) {
        eventoEn(EstadoEvento.PUBLICADO);
        when(productoras.puedeValidarEntradas(PRODUCTORA, VALIDADOR)).thenReturn(true);
        var usadoEn = OffsetDateTime.now();
        when(tickets.validarYUsar(QR, EVENTO)).thenReturn(new UsoTicketDTO(
                UsoTicketDTO.Resultado.valueOf(resultado.name()), 77L, EVENTO, "General", usadoEn));

        ValidacionDTO validacion = servicio.validarEntrada(new ValidarEntradaRequest(EVENTO, QR), VALIDADOR);

        assertThat(validacion.resultado()).isEqualTo(resultado);
        assertThat(validacion.mensaje()).isEqualTo(mensaje);
        if (conTicket) {
            assertThat(validacion.ticket()).isEqualTo(
                    new ValidacionDTO.TicketValidado(77L, EVENTO, "General", usadoEn));
        } else {
            assertThat(validacion.ticket()).isNull();
        }
        assertThat(auditada()).extracting("resultado", "ticketId", "eventoId", "validadorId")
                .containsExactly(resultado, 77L, EVENTO, VALIDADOR);
    }

    @ParameterizedTest(name = "evento {0} -> INVALIDO sin consumir el ticket")
    @CsvSource({"BORRADOR", "CANCELADO"})
    void unEventoNoPublicadoNoAdmiteIngresos(EstadoEvento estado) {
        eventoEn(estado);
        when(productoras.puedeValidarEntradas(PRODUCTORA, VALIDADOR)).thenReturn(true);

        ValidacionDTO validacion = servicio.validarEntrada(new ValidarEntradaRequest(EVENTO, QR), VALIDADOR);

        assertThat(validacion.resultado()).isEqualTo(Resultado.INVALIDO);
        assertThat(validacion.mensaje()).isEqualTo("Este evento no admite ingresos.");
        verifyNoInteractions(tickets);
        assertThat(auditada()).extracting("resultado", "ticketId").containsExactly(Resultado.INVALIDO, null);
    }

    @Test
    void quienNoEsMiembroDeLaProductoraNoValidaNiDejaRastro() {
        eventoEn(EstadoEvento.PUBLICADO);
        when(productoras.puedeValidarEntradas(PRODUCTORA, VALIDADOR)).thenReturn(false);

        assertThatThrownBy(() -> servicio.validarEntrada(new ValidarEntradaRequest(EVENTO, QR), VALIDADOR))
                .isInstanceOf(NoEsMiembroDeLaProductoraException.class);

        verifyNoInteractions(tickets, validaciones);
    }

    private void eventoEn(EstadoEvento estado) {
        when(eventos.consultarEventoPublico(EVENTO)).thenReturn(new EventoDTO(
                EVENTO, new OrganizadorDeEventoDTO(PRODUCTORA, "Aurora"), "Fiesta", null,
                OffsetDateTime.now().plusDays(1), "Club", estado, List.of(), null));
    }

    private Validacion auditada() {
        var captor = ArgumentCaptor.forClass(Validacion.class);
        verify(validaciones).save(captor.capture());
        return captor.getValue();
    }
}
