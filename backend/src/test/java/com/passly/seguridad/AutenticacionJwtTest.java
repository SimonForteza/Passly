package com.passly.seguridad;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import com.passly.productoras.ProductoraService;
import com.passly.productoras.dto.CrearProductoraRequest;
import com.passly.usuarios.Rol;
import com.passly.usuarios.UsuarioService;
import com.passly.usuarios.dto.CrearUsuarioRequest;
import com.passly.usuarios.dto.UsuarioDTO;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autenticacion con JWT de punta a punta (PAS-33): {@code POST /api/auth/login} emite el token y el
 * resto de la API lo exige en {@code Authorization: Bearer}.
 *
 * <p>A diferencia del resto de los tests de integracion, que inyectan la identidad con
 * {@code user(...)} de spring-security-test, aca todo pasa por el filtro real de Bearer: es lo que
 * la catedra prueba en vivo ("JWT invalidos"). Cada caso de token rechazado corresponde a un
 * ataque concreto: payload modificado, firma con otra clave, {@code alg: none}, vencido, emitido
 * por otro sistema, o de una cuenta que no existe.
 */
@SpringBootTest
class AutenticacionJwtTest {

    private static final String CLAVE = "passly1234";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ProductoraService productoraService;

    /** El mismo bean que usa el login: firma con la clave real del backend. */
    @Autowired
    private JwtEncoder jwtEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // ---------------------------------------------------------------- login

    @Test
    void elLoginDevuelveUnTokenQueIdentificaAlUsuario() throws Exception {
        UsuarioDTO comprador = registrar(Rol.COMPRADOR);

        login(comprador.email(), CLAVE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEn").exists())
                .andExpect(jsonPath("$.usuario.id").value(comprador.id()))
                .andExpect(jsonPath("$.usuario.rol").value("COMPRADOR"));

        mockMvc.perform(get("/api/usuarios/me").header(HttpHeaders.AUTHORIZATION, bearer(comprador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(comprador.id()))
                .andExpect(jsonPath("$.email").value(comprador.email()));
    }

    @Test
    void unaClaveIncorrectaYUnEmailInexistenteDanElMismo401() throws Exception {
        UsuarioDTO comprador = registrar(Rol.COMPRADOR);

        login(comprador.email(), "otra-clave")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Email o contraseña inválidos."));
        login("nadie-" + UUID.randomUUID() + "@test.passly", CLAVE)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Email o contraseña inválidos."));
    }

    @Test
    void unLoginSinContraseñaEsUn400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alguien@test.passly\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------- tokens rechazados

    @Test
    void sinTokenUnaRutaProtegidaDa401EnProblemDetail() throws Exception {
        mockMvc.perform(get("/api/usuarios/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Sesión ausente, inválida o vencida. Iniciá sesión de nuevo."));
    }

    @Test
    void unTokenConElPayloadModificadoSeRechaza() throws Exception {
        UsuarioDTO victima = registrar(Rol.COMPRADOR);
        UsuarioDTO atacante = registrar(Rol.COMPRADOR);
        String[] partes = tokenDe(atacante).split("\\.");

        // El atacante cambia el sub por el id de otra cuenta y conserva su firma.
        String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8)
                .replace("\"sub\":\"" + atacante.id() + "\"", "\"sub\":\"" + victima.id() + "\"");
        String adulterado = partes[0] + "." + base64Url(payload) + "." + partes[2];

        conToken(adulterado).andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenFirmadoConOtraClaveSeRechaza() throws Exception {
        UsuarioDTO comprador = registrar(Rol.COMPRADOR);
        JwtEncoder otraClave = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(new SecretKeySpec(
                "una-clave-distinta-de-al-menos-32-bytes!!".getBytes(StandardCharsets.UTF_8), "HmacSHA256")));

        conToken(firmar(otraClave, claimsValidos(comprador).build())).andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenSinFirmaConAlgNoneSeRechaza() throws Exception {
        UsuarioDTO comprador = registrar(Rol.COMPRADOR);
        String payload = "{\"iss\":\"passly\",\"sub\":\"" + comprador.id() + "\",\"exp\":"
                + Instant.now().plus(1, ChronoUnit.HOURS).getEpochSecond() + "}";

        conToken(base64Url("{\"alg\":\"none\"}") + "." + base64Url(payload) + ".")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenVencidoSeRechaza() throws Exception {
        UsuarioDTO comprador = registrar(Rol.COMPRADOR);
        Instant haceUnaHora = Instant.now().minus(1, ChronoUnit.HOURS);
        JwtClaimsSet vencido = claimsValidos(comprador)
                .issuedAt(haceUnaHora.minus(2, ChronoUnit.HOURS))
                .expiresAt(haceUnaHora)
                .build();

        conToken(firmar(jwtEncoder, vencido)).andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenDeOtroEmisorSeRechaza() throws Exception {
        UsuarioDTO comprador = registrar(Rol.COMPRADOR);

        conToken(firmar(jwtEncoder, claimsValidos(comprador).issuer("otro-sistema").build()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenDeUnaCuentaQueNoExisteSeRechaza() throws Exception {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("passly")
                .subject("999999999")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

        conToken(firmar(jwtEncoder, claims)).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------ autorizacion

    @Test
    void unCompradorConTokenValidoNoPuedeCrearEventos() throws Exception {
        UsuarioDTO comprador = registrar(Rol.COMPRADOR);

        crearEvento(bearer(comprador), 1L).andExpect(status().isForbidden());
    }

    /**
     * El rol no sale del token sino de Usuarios (ver {@code ConvertidorDeJwtAAutenticacion}): quien
     * crea su productora pasa a ORGANIZADOR y puede crear su primer evento con el mismo token, sin
     * volver a iniciar sesion. Es el recorrido "Crear mi productora -> Crear mi primer evento".
     */
    @Test
    void elRolSeLeeActualizadoAunqueElTokenSeaAnterior() throws Exception {
        UsuarioDTO usuario = registrar(Rol.COMPRADOR);
        String token = bearer(usuario);
        crearEvento(token, 1L).andExpect(status().isForbidden());

        Long idProductora = productoraService.crearProductora(
                new CrearProductoraRequest("Productora " + UUID.randomUUID(), null, null, null),
                usuario.id()).id();

        crearEvento(token, idProductora).andExpect(status().isCreated());
    }

    // ---------------------------------------------------------------- helpers

    private UsuarioDTO registrar(Rol rol) {
        return usuarioService.registrarUsuario(new CrearUsuarioRequest(
                "jwt-" + UUID.randomUUID() + "@test.passly", "Usuario JWT", CLAVE, rol));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private String tokenDe(UsuarioDTO usuario) throws Exception {
        String cuerpo = login(usuario.email(), CLAVE)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(cuerpo, "$.token");
    }

    private String bearer(UsuarioDTO usuario) throws Exception {
        return "Bearer " + tokenDe(usuario);
    }

    private ResultActions conToken(String token) throws Exception {
        return mockMvc.perform(get("/api/usuarios/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private ResultActions crearEvento(String bearer, Long idProductora) throws Exception {
        return mockMvc.perform(post("/api/eventos")
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "idProductora": %d,
                          "nombre": "Festival JWT",
                          "fechaHora": "2030-01-01T20:00:00Z",
                          "lugar": "Parque Centenario, CABA",
                          "tiposEntrada": [ { "nombre": "General", "precio": 15000.00, "cupoTotal": 200 } ]
                        }
                        """.formatted(idProductora)));
    }

    private static JwtClaimsSet.Builder claimsValidos(UsuarioDTO usuario) {
        return JwtClaimsSet.builder()
                .issuer("passly")
                .subject(String.valueOf(usuario.id()))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
    }

    private static String firmar(JwtEncoder encoder, JwtClaimsSet claims) {
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private static String base64Url(String texto) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }
}
