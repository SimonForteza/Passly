package com.passly.tickets.internal.negocio;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

/** Firma el identificador del ticket y codifica el contenido resultante como PNG. */
@Component
class GeneradorDeQr {

    private static final String PREFIJO = "PASSLY:v1:";
    private static final String ALGORITMO = "HmacSHA256";

    private final byte[] secreto;

    GeneradorDeQr(@Value("${passly.tickets.secreto-qr}") String secreto) {
        if (secreto == null || secreto.length() < 32) {
            throw new IllegalArgumentException("passly.tickets.secreto-qr debe tener al menos 32 caracteres");
        }
        this.secreto = secreto.getBytes(StandardCharsets.UTF_8);
    }

    String crearContenido(UUID codigo) {
        String datos = PREFIJO + codigo;
        return datos + ":" + firmar(datos);
    }

    boolean esValido(String contenido) {
        if (contenido == null || !contenido.startsWith(PREFIJO)) return false;
        int separador = contenido.lastIndexOf(':');
        if (separador <= PREFIJO.length()) return false;

        String datos = contenido.substring(0, separador);
        String firmaRecibida = contenido.substring(separador + 1);
        byte[] esperada = firmar(datos).getBytes(StandardCharsets.US_ASCII);
        byte[] recibida = firmaRecibida.getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(esperada, recibida);
    }

    String comoPngBase64(String contenido) {
        try {
            var matriz = new QRCodeWriter().encode(contenido, BarcodeFormat.QR_CODE, 280, 280);
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matriz, "PNG", salida);
            return Base64.getEncoder().encodeToString(salida.toByteArray());
        } catch (WriterException | IOException ex) {
            throw new IllegalStateException("No se pudo generar el QR del ticket", ex);
        }
    }

    private String firmar(String datos) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(secreto, ALGORITMO));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(datos.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("No se pudo firmar el QR", ex);
        }
    }
}
