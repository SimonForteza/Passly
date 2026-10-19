package com.passly.notificaciones.internal;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import java.time.OffsetDateTime;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class DespachadorDeCorreoTest {
    @Test void smtpCaidoReintentaSinPerderCorreoYLuegoEntrega() {
        var repository=mock(CorreoRepository.class); var smtp=mock(JavaMailSender.class);
        var correo=new Correo("persona@test.com","Invitación","Contenido");
        when(repository.pendientes(any())).thenReturn(List.of(correo));
        doThrow(new MailSendException("Sin conexión")).doNothing().when(smtp).send(any(SimpleMailMessage.class));
        var worker=new DespachadorDeCorreo(repository,smtp,"passly@test.com");
        worker.enviarPendientes();
        assertThat(correo.estado).isEqualTo("REINTENTO");
        assertThat(correo.proximoIntento).isAfter(OffsetDateTime.now());
        worker.enviarPendientes();
        assertThat(correo.estado).isEqualTo("ENVIADO"); assertThat(correo.intentos).isEqualTo(2);
    }
}
