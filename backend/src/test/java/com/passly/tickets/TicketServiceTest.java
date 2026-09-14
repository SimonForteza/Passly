package com.passly.tickets;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.passly.tickets.dto.EmitirTicketsRequest;
import com.passly.tickets.dto.LineaDeEmisionDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TicketServiceTest {

    @Autowired
    private TicketService ticketService;

    @Test
    void unaLineaDeTresUnidadesEmiteTresQrUnicosValidosYDecodificables() throws Exception {
        long idOrden = ThreadLocalRandom.current().nextLong(1_000_000, Long.MAX_VALUE);
        ticketService.emitirTickets(new EmitirTicketsRequest(
                idOrden,
                99L,
                List.of(new LineaDeEmisionDTO(10L, 20L, "General", 3))));
        var emitidos = ticketService.listarTicketsDeOrden(idOrden, 99L);

        assertThat(emitidos).hasSize(3);
        assertThat(emitidos).extracting(ticket -> ticket.codigo()).doesNotHaveDuplicates();
        assertThat(emitidos).extracting(ticket -> ticket.qrBase64()).doesNotHaveDuplicates();

        for (var ticket : emitidos) {
            String contenido = decodificar(ticket.qrBase64());
            assertThat(ticketService.esContenidoQrValido(contenido)).isTrue();
            assertThat(ticketService.esContenidoQrValido(contenido + "alterado")).isFalse();
        }
    }

    private static String decodificar(String base64) throws Exception {
        byte[] png = Base64.getDecoder().decode(base64);
        var imagen = ImageIO.read(new ByteArrayInputStream(png));
        var fuente = new BufferedImageLuminanceSource(imagen);
        return new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(fuente))).getText();
    }
}
