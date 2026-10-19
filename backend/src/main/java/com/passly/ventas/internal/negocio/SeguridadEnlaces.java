package com.passly.ventas.internal.negocio;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
final class SeguridadEnlaces {
    private SeguridadEnlaces() {}
    static String token() { byte[] bytes=new byte[32]; new SecureRandom().nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    static String hash(String texto) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
