package com.passly.seguridad.internal;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Firma y verificacion de los JWT de Passly (PAS-33).
 *
 * <p><b>Por que HS256 (clave simetrica) y no RS256.</b> El mismo proceso emite y valida el token:
 * no hay un tercero que necesite verificarlo sin poder firmarlo, que es lo que justificaria un par
 * de claves publica/privada. Con un solo backend, una clave HMAC de al menos 256 bits alcanza y
 * es una sola variable de entorno ({@code PASSLY_JWT_SECRET}). Si algun dia otro servicio tuviera
 * que validar tokens de Passly, ese es el momento de pasar a RS256 y publicar la clave publica.
 *
 * <p><b>El decoder solo acepta HS256 y el emisor "passly".</b> Un token con {@code alg: none},
 * firmado con otro algoritmo, con otra clave, vencido o emitido por otro sistema se rechaza con
 * 401 antes de llegar a cualquier controller. Son los "JWT invalidos" que la catedra prueba en vivo.
 */
@Configuration
class ConfiguracionDeJwt {

    /** Valor del claim {@code iss}: el decoder rechaza tokens que no lo traigan. */
    static final String EMISOR = "passly";

    private static final int MINIMO_DE_BYTES = 32;

    private final SecretKey clave;

    ConfiguracionDeJwt(@Value("${passly.seguridad.jwt.secreto}") String secreto) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MINIMO_DE_BYTES) {
            // Fallar al arrancar es mejor que firmar con una clave adivinable: HS256 exige 256 bits.
            throw new IllegalStateException(
                    "passly.seguridad.jwt.secreto (PASSLY_JWT_SECRET) debe tener al menos "
                            + MINIMO_DE_BYTES + " bytes; tiene " + bytes.length);
        }
        this.clave = new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder codificadorDeJwt() {
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(clave));
    }

    @Bean
    JwtDecoder decodificadorDeJwt() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(clave)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // createDefaultWithIssuer = vencimiento (exp/nbf, con 60 s de tolerancia) + emisor.
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(EMISOR));
        return decoder;
    }
}
