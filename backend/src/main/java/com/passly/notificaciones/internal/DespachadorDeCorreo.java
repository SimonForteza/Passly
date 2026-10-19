package com.passly.notificaciones.internal;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;

@Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="passly.correo.activo",havingValue="true",matchIfMissing=true)
@EnableScheduling
class DespachadorDeCorreo {
    private final CorreoRepository repository;
    private final JavaMailSender sender;
    private final String remitente;
    DespachadorDeCorreo(CorreoRepository repository, JavaMailSender sender,
            @Value("${passly.correo.remitente:entradas@passly.test}") String remitente) {
        this.repository=repository; this.sender=sender; this.remitente=remitente;
    }
    // Transacción del worker: nunca mantiene abierta la transacción de compra/alta.
    // SKIP LOCKED permite múltiples workers. SMTP puede entregar dos veces si el proceso cae tras enviar.
    @Scheduled(fixedDelayString="${passly.correo.intervalo-ms:10000}")
    @Transactional
    public void enviarPendientes() {
        for (Correo correo : repository.pendientes(OffsetDateTime.now())) {
            try {
                var mensaje=new SimpleMailMessage(); mensaje.setFrom(remitente);
                mensaje.setTo(correo.destinatario); mensaje.setSubject(correo.asunto); mensaje.setText(correo.cuerpo);
                sender.send(mensaje); correo.estado="ENVIADO";
            } catch (org.springframework.mail.MailException e) {
                correo.estado="REINTENTO";
                correo.proximoIntento=OffsetDateTime.now().plusSeconds(Math.min(3600,30L << Math.min(correo.intentos,6)));
            }
            correo.intentos++;
        }
    }
}
