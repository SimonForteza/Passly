package com.passly.tickets.internal.negocio;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GeneradorDeQrTest {

    private final GeneradorDeQr generador =
            new GeneradorDeQr("secreto-de-prueba-con-mas-de-32-caracteres");

    @Test
    void elPngSePuedeDecodificarYLaFirmaDetectaAlteraciones() throws Exception {
        String contenido = generador.crearContenido(UUID.randomUUID());
        String decodificado = decodificar(generador.comoPngBase64(contenido));

        assertThat(decodificado).isEqualTo(contenido);
        assertThat(generador.esValido(decodificado)).isTrue();
        assertThat(generador.esValido(decodificado + "alterado")).isFalse();
    }

    @Test
    void dosTicketsProducenContenidosYQrDiferentes() {
        String uno = generador.crearContenido(UUID.randomUUID());
        String dos = generador.crearContenido(UUID.randomUUID());

        assertThat(uno).isNotEqualTo(dos);
        assertThat(generador.comoPngBase64(uno)).isNotEqualTo(generador.comoPngBase64(dos));
    }

    private static String decodificar(String base64) throws Exception {
        byte[] png = Base64.getDecoder().decode(base64);
        var imagen = ImageIO.read(new ByteArrayInputStream(png));
        var fuente = new BufferedImageLuminanceSource(imagen);
        return new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(fuente))).getText();
    }
}
