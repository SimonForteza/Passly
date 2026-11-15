package com.passly.productoras.internal.negocio;

import com.passly.notificaciones.CorreoService;
import com.passly.productoras.RolEnProductora;
import com.passly.productoras.internal.datos.Invitacion;
import com.passly.productoras.internal.datos.InvitacionRepository;
import com.passly.productoras.internal.datos.Miembro;
import com.passly.productoras.internal.datos.Productora;
import com.passly.productoras.internal.datos.ProductoraRepository;
import com.passly.usuarios.Rol;
import com.passly.usuarios.UsuarioService;
import com.passly.usuarios.dto.UsuarioDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unitario puro de las invitaciones a una productora (PAS-38): el enlace dura siete dias, admite
 * una sola aceptacion y exige entrar con el email invitado (CLAUDE.md 1, "Alta de productoras").
 *
 * <p>El token se guarda hasheado y el hash es privado, asi que los finders del repositorio se
 * stubbean con {@code anyString()}: lo que se prueba es la regla, no el SHA-256.
 */
class InvitacionServiceTest {

    private static final Long PRODUCTORA = 1L;
    private static final Long DUENIO = 10L;
    private static final Long INVITADA = 20L;
    private static final String EMAIL = "staff@passly.test";

    private final InvitacionRepository invitaciones = mock(InvitacionRepository.class);
    private final ProductoraRepository productoras = mock(ProductoraRepository.class);
    private final UsuarioService usuarios = mock(UsuarioService.class);
    private final CorreoService correos = mock(CorreoService.class);
    private final InvitacionService servicio =
            new InvitacionService(invitaciones, productoras, usuarios, correos, "http://localhost:5173");

    private Productora aurora;

    @BeforeEach
    void productoraConDuenio() {
        aurora = new Productora("Aurora", null, null, null);
        aurora.incorporar(new Miembro(DUENIO, RolEnProductora.DUENIO));
        when(productoras.bloquear(PRODUCTORA)).thenReturn(Optional.of(aurora));
    }

    @ParameterizedTest(name = "invitacion como {0} habilita {1}")
    @CsvSource({"STAFF, ORGANIZADOR", "VALIDADOR, VALIDADOR"})
    void aceptarUnaPendienteIncorporaConElRolGlobalQueCorresponde(RolEnProductora rol, Rol capacidad) {
        var invitacion = invitacion(rol, "PENDIENTE", OffsetDateTime.now().plusDays(3));
        quienAcepta(EMAIL);

        assertThat(servicio.aceptar("token", INVITADA)).isEqualTo(PRODUCTORA);

        verify(usuarios).habilitarCapacidad(INVITADA, capacidad);
        assertThat(aurora.tieneMiembroConRol(INVITADA, rol)).isTrue();
        assertThat(invitacion.estado).isEqualTo("ACEPTADA");
    }

    @Test
    void elEmailNoDistingueMayusculas() {
        invitacion(RolEnProductora.STAFF, "PENDIENTE", OffsetDateTime.now().plusDays(3));
        quienAcepta("Staff@Passly.TEST");

        assertThat(servicio.aceptar("token", INVITADA)).isEqualTo(PRODUCTORA);
    }

    @Test
    void unaInvitacionVencidaResponde410YNoIncorpora() {
        var invitacion = invitacion(RolEnProductora.STAFF, "PENDIENTE", OffsetDateTime.now().minusMinutes(1));
        quienAcepta(EMAIL);

        assertThatThrownBy(() -> servicio.aceptar("token", INVITADA)).satisfies(estado(HttpStatus.GONE));

        assertThat(aurora.tieneMiembro(INVITADA)).isFalse();
        assertThat(invitacion.estado).isEqualTo("PENDIENTE");
        verify(usuarios, never()).habilitarCapacidad(anyLong(), any());
    }

    @Test
    void unaInvitacionRevocadaResponde410() {
        invitacion(RolEnProductora.STAFF, "REVOCADA", OffsetDateTime.now().plusDays(3));
        quienAcepta(EMAIL);

        assertThatThrownBy(() -> servicio.aceptar("token", INVITADA)).satisfies(estado(HttpStatus.GONE));
    }

    @Test
    void conOtroEmailResponde403AunqueTengaElEnlace() {
        invitacion(RolEnProductora.STAFF, "PENDIENTE", OffsetDateTime.now().plusDays(3));
        quienAcepta("otra@passly.test");

        assertThatThrownBy(() -> servicio.aceptar("token", INVITADA)).satisfies(estado(HttpStatus.FORBIDDEN));

        assertThat(aurora.tieneMiembro(INVITADA)).isFalse();
    }

    @Test
    void aceptarDosVecesEsIdempotenteYNoVuelveAIncorporar() {
        invitacion(RolEnProductora.STAFF, "ACEPTADA", OffsetDateTime.now().minusDays(30));
        quienAcepta(EMAIL);

        assertThat(servicio.aceptar("token", INVITADA)).isEqualTo(PRODUCTORA);

        verify(usuarios, never()).habilitarCapacidad(anyLong(), any());
        assertThat(aurora.tieneMiembro(INVITADA)).isFalse();
    }

    @Test
    void unTokenDesconocidoResponde404() {
        when(invitaciones.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.aceptar("inventado", INVITADA)).satisfies(estado(HttpStatus.NOT_FOUND));
        verifyNoInteractions(usuarios);
    }

    @Test
    void soloElDuenioCreaInvitaciones() {
        assertThatThrownBy(() -> servicio.crear(PRODUCTORA, INVITADA, EMAIL, RolEnProductora.STAFF))
                .satisfies(estado(HttpStatus.FORBIDDEN));
        verifyNoInteractions(correos);
    }

    @Test
    void noSeInvitaComoDuenio() {
        assertThatThrownBy(() -> servicio.crear(PRODUCTORA, DUENIO, EMAIL, RolEnProductora.DUENIO))
                .satisfies(estado(HttpStatus.BAD_REQUEST));
    }

    @Test
    void crearEncolaElCorreoConUnEnlaceQueVenceEnSieteDias() {
        when(usuarios.consultarUsuario(DUENIO)).thenReturn(new UsuarioDTO(DUENIO, "duenio@passly.test", "D", Rol.ORGANIZADOR));
        when(invitaciones.findByProductoraIdOrderByIdDesc(PRODUCTORA)).thenReturn(List.of());
        when(invitaciones.save(any(Invitacion.class))).thenAnswer(i -> i.getArgument(0));

        var vista = servicio.crear(PRODUCTORA, DUENIO, "  Staff@Passly.test ", RolEnProductora.STAFF);

        assertThat(vista.email()).isEqualTo(EMAIL);
        assertThat(vista.estado()).isEqualTo("PENDIENTE");
        assertThat(vista.venceEn()).isBetween(OffsetDateTime.now().plusDays(6), OffsetDateTime.now().plusDays(8));
        verify(correos).encolar(org.mockito.ArgumentMatchers.eq(EMAIL), anyString(),
                org.mockito.ArgumentMatchers.startsWith("Te invitaron a Aurora como STAFF"));
    }

    private Invitacion invitacion(RolEnProductora rol, String estado, OffsetDateTime venceEn) {
        var invitacion = new Invitacion(PRODUCTORA, EMAIL, rol);
        invitacion.estado = estado;
        invitacion.venceEn = venceEn;
        when(invitaciones.findByTokenHash(anyString())).thenReturn(Optional.of(invitacion));
        when(invitaciones.bloquearPorToken(anyString())).thenReturn(Optional.of(invitacion));
        return invitacion;
    }

    private void quienAcepta(String email) {
        when(usuarios.consultarUsuario(INVITADA)).thenReturn(new UsuarioDTO(INVITADA, email, "Sofia", Rol.COMPRADOR));
    }

    private static java.util.function.Consumer<Throwable> estado(HttpStatus esperado) {
        return error -> assertThat(error).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(esperado));
    }
}
