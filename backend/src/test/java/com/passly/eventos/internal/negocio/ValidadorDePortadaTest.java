package com.passly.eventos.internal.negocio;

import com.passly.eventos.ArchivoDePortadaInvalidoException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unitario puro: el tipo de la portada sale de la firma del archivo, no de lo que diga el cliente (PAS-47). */
class ValidadorDePortadaTest {

    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0};
    static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '};

    @Test
    void detectaJpegPngYWebpPorSusPrimerosBytes() {
        assertThat(ValidadorDePortada.tipoDe(JPEG)).isEqualTo("image/jpeg");
        assertThat(ValidadorDePortada.tipoDe(PNG)).isEqualTo("image/png");
        assertThat(ValidadorDePortada.tipoDe(WEBP)).isEqualTo("image/webp");
    }

    @Test
    void rechazaUnHtmlAunqueSeLlameFotoJpg() {
        byte[] html = "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> ValidadorDePortada.tipoDe(html))
                .isInstanceOf(ArchivoDePortadaInvalidoException.class);
    }

    @Test
    void rechazaSvgPorquePuedeLlevarScripts() {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> ValidadorDePortada.tipoDe(svg))
                .isInstanceOf(ArchivoDePortadaInvalidoException.class);
    }

    @Test
    void rechazaUnRiffQueNoEsWebp() {
        byte[] wav = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'A', 'V', 'E'};
        assertThatThrownBy(() -> ValidadorDePortada.tipoDe(wav))
                .isInstanceOf(ArchivoDePortadaInvalidoException.class);
    }

    @Test
    void rechazaVacioYMasDeDosMegas() {
        assertThatThrownBy(() -> ValidadorDePortada.tipoDe(new byte[0]))
                .isInstanceOf(ArchivoDePortadaInvalidoException.class);

        byte[] grande = new byte[ValidadorDePortada.TAMANIO_MAXIMO + 1];
        System.arraycopy(JPEG, 0, grande, 0, JPEG.length);
        assertThatThrownBy(() -> ValidadorDePortada.tipoDe(grande))
                .isInstanceOf(ArchivoDePortadaInvalidoException.class)
                .hasMessageContaining("2 MB");
    }
}
