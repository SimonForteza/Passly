package com.passly;

import com.passly.usuarios.*;
import com.passly.usuarios.dto.*;
import com.passly.productoras.*;
import com.passly.productoras.dto.*;
import com.passly.eventos.*;
import com.passly.eventos.dto.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties={
    "server.port=18082",
    "passly.pagos.pasarela.base-url=http://localhost:18082",
    "passly.facturacion.consumidor-activo=false",
    "passly.correo.activo=false",
    "spring.jpa.show-sql=false"})
class AltasInvitadosIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired UsuarioService usuarios;
    @Autowired ProductoraService productoras;
    @Autowired EventoService eventos;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;
    @Autowired com.passly.facturacion.FacturacionService facturacion;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean com.passly.pagos.PagoService pagos;
    MockMvc mvc;
    UsuarioDTO duenio;
    Long productora;
    @BeforeEach void preparar() {
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity())
            .addFilters((jakarta.servlet.Filter)context.getBean("origenInvitadoFilter")).build();
        duenio=cuenta(Rol.COMPRADOR);
        productora=productoras.crearProductora(new CrearProductoraRequest("Productora "+UUID.randomUUID(),null,null,null),duenio.id()).id();
    }
    UsuarioDTO cuenta(Rol rol) {
        return usuarios.registrarUsuario(new CrearUsuarioRequest(UUID.randomUUID()+"@test.com","Persona","password123",rol));
    }
    String actuar(UsuarioDTO usuario) { return usuario.id().toString(); }
    String cuerpo(MvcResult resultado) throws Exception { return resultado.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8); }
    long crearInvitacion(String email,String rol) throws Exception {
        var resultado=mvc.perform(post("/api/productoras/{id}/invitaciones",productora)
            .with(user(actuar(duenio)).roles("ORGANIZADOR")).contentType(APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email",email,"rolEnProductora",rol))))
            .andExpect(status().isOk()).andReturn();
        return json.readTree(cuerpo(resultado)).get("id").asLong();
    }
    String token(long invitacion) {
        String texto=jdbc.queryForObject("select c.cuerpo from notificaciones.correo c join productoras.invitacion i on i.correo_id=c.id where i.id=?",String.class,invitacion);
        return texto.substring(texto.lastIndexOf('#')+1);
    }
    MvcResult aceptar(String token,UsuarioDTO usuario) throws Exception {
        return mvc.perform(post("/api/invitaciones/aceptacion").header("X-Invitacion-Token",token)
            .with(user(actuar(usuario)).roles(usuario.rol().name()))).andReturn();
    }
    long tipo(BigDecimal precio,int cupo) {
        var e=eventos.crearEvento(new CrearEventoRequest(productora,"Fiesta",null,OffsetDateTime.now().plusDays(2),"Salón",
            List.of(new CrearTipoEntradaRequest("General",precio,cupo))),duenio.id());
        eventos.publicarEvento(e.id(),duenio.id()); return e.tiposEntrada().getFirst().id();
    }
    void agregar(MockHttpSession sesion,long tipo) throws Exception {
        mvc.perform(post("/api/ventas/invitados/carrito/items").session(sesion).contentType(APPLICATION_JSON)
            .content("{\"idTipoEntrada\":"+tipo+",\"cantidad\":1}")).andExpect(status().isOk());
    }
    MvcResult comprar(MockHttpSession sesion,String clave,String email) throws Exception {
        return comprar(sesion,clave,email,"4242424242424242");
    }
    MvcResult comprar(MockHttpSession sesion,String clave,String email,String numeroTarjeta) throws Exception {
        return mvc.perform(post("/api/ventas/invitados/ordenes").session(sesion).header("Idempotency-Key",clave)
            .contentType(APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                "nombre","Invitado","email",email,
                "tarjeta",Map.of("numero",numeroTarjeta,"titular","Invitado","vencimiento","12/30","codigoSeguridad","123"))))).andReturn();
    }
    @Test void altaAutonomaOtorgaCapacidadYDueno() {
        assertThat(usuarios.consultarUsuario(duenio.id()).rol()).isEqualTo(Rol.ORGANIZADOR);
        assertThat(productoras.listarMiembros(productora,duenio.id())).anySatisfy(m->{
            assertThat(m.idUsuario()).isEqualTo(duenio.id()); assertThat(m.rolEnProductora()).isEqualTo(RolEnProductora.DUENIO);
        });
    }
    @Test void altaInvalidaODuplicadaNoPromueveUsuario() throws Exception {
        var comprador=cuenta(Rol.COMPRADOR);
        mvc.perform(post("/api/productoras").with(user(actuar(comprador)).roles("COMPRADOR"))
            .contentType(APPLICATION_JSON).content("{\"nombreComercial\":\"\"}")).andExpect(status().isBadRequest());
        String nombre=productoras.consultarProductora(productora).nombreComercial();
        mvc.perform(post("/api/productoras").with(user(actuar(comprador)).roles("COMPRADOR"))
            .contentType(APPLICATION_JSON).content(json.writeValueAsString(Map.of("nombreComercial",nombre))))
            .andExpect(status().isConflict());
        assertThat(usuarios.consultarUsuario(comprador.id()).rol()).isEqualTo(Rol.COMPRADOR);
    }
    @Test void invitacionAceptaCuentaNuevaYRepeticionEsInocua() throws Exception {
        String email=UUID.randomUUID()+"@test.com";
        long id=crearInvitacion(email.toUpperCase(Locale.ROOT),"STAFF");
        var persona=usuarios.registrarUsuario(new CrearUsuarioRequest(email,"Nueva","password123",Rol.COMPRADOR));
        String token=token(id);
        mvc.perform(get("/api/invitaciones").header("X-Invitacion-Token",token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.idProductora").value(productora))
            .andExpect(jsonPath("$.email").value(email));
        assertThat(aceptar(token,persona).getResponse().getStatus()).isEqualTo(200);
        assertThat(aceptar(token,persona).getResponse().getStatus()).isEqualTo(200);
        assertThat(usuarios.consultarUsuario(persona.id()).rol()).isEqualTo(Rol.ORGANIZADOR);
        assertThat(productoras.listarMiembros(productora,duenio.id())).filteredOn(m->m.idUsuario().equals(persona.id())).hasSize(1);
    }
    @Test void validadorConservaCapacidadDeOrganizador() throws Exception {
        var persona=cuenta(Rol.ORGANIZADOR);
        assertThat(aceptar(token(crearInvitacion(persona.email(),"VALIDADOR")),persona).getResponse().getStatus()).isEqualTo(200);
        assertThat(usuarios.consultarUsuario(persona.id()).rol()).isEqualTo(Rol.ORGANIZADOR);
    }
    @Test void soloDuenoGestionaInvitacionesYSinCopropietarios() throws Exception {
        var ajeno=cuenta(Rol.ORGANIZADOR);
        mvc.perform(get("/api/productoras/{id}/invitaciones",productora).with(user(actuar(ajeno)).roles("ORGANIZADOR")))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/productoras/{id}/invitaciones",productora).with(user(actuar(duenio)).roles("ORGANIZADOR"))
            .contentType(APPLICATION_JSON).content("{\"email\":\"nuevo@test.com\",\"rolEnProductora\":\"DUENIO\"}"))
            .andExpect(status().isBadRequest());
    }
    @Test void noDuplicaPendientesYReenvioInvalidaTokenAnterior() throws Exception {
        var persona=cuenta(Rol.COMPRADOR); long id=crearInvitacion(persona.email(),"STAFF"); String anterior=token(id);
        mvc.perform(post("/api/productoras/{id}/invitaciones",productora).with(user(actuar(duenio)).roles("ORGANIZADOR"))
            .contentType(APPLICATION_JSON).content(json.writeValueAsString(Map.of("email",persona.email(),"rolEnProductora","STAFF"))))
            .andExpect(status().isConflict());
        mvc.perform(post("/api/productoras/{id}/invitaciones/{i}/reenvio",productora,id)
            .with(user(actuar(duenio)).roles("ORGANIZADOR"))).andExpect(status().isOk());
        assertThat(aceptar(anterior,persona).getResponse().getStatus()).isEqualTo(404);
        assertThat(aceptar(token(id),persona).getResponse().getStatus()).isEqualTo(200);
    }
    @Test void rechazaEmailIncorrectoVencimientoYRevocacion() throws Exception {
        var persona=cuenta(Rol.COMPRADOR); var ajeno=cuenta(Rol.COMPRADOR);
        long id=crearInvitacion(persona.email(),"VALIDADOR"); String token=token(id);
        assertThat(aceptar(token,ajeno).getResponse().getStatus()).isEqualTo(403);
        jdbc.update("update productoras.invitacion set vence_en=current_timestamp-interval '1 day' where id=?",id);
        assertThat(aceptar(token,persona).getResponse().getStatus()).isEqualTo(410);
        mvc.perform(delete("/api/productoras/{id}/invitaciones/{i}",productora,id).with(user(actuar(duenio)).roles("ORGANIZADOR")))
            .andExpect(status().isNoContent());
        assertThat(aceptar(token,persona).getResponse().getStatus()).isEqualTo(410);
        assertThat(usuarios.consultarUsuario(persona.id()).rol()).isEqualTo(Rol.COMPRADOR);
    }
    @Test void aceptacionConcurrenteAgregaUnaSolaMembresia() throws Exception {
        var persona=cuenta(Rol.COMPRADOR); String token=token(crearInvitacion(persona.email(),"STAFF"));
        try (var pool=Executors.newFixedThreadPool(2)) {
            var uno=pool.submit(()->aceptar(token,persona).getResponse().getStatus());
            var dos=pool.submit(()->aceptar(token,persona).getResponse().getStatus());
            assertThat(uno.get(15,TimeUnit.SECONDS)).isEqualTo(200); assertThat(dos.get(15,TimeUnit.SECONDS)).isEqualTo(200);
        }
        assertThat(productoras.listarMiembros(productora,duenio.id())).filteredOn(m->m.idUsuario().equals(persona.id())).hasSize(1);
    }
    @Test void compraSinCuentaEmiteQrYReintentoNoDuplica() throws Exception {
        var sesion=new MockHttpSession(); long tipo=tipo(BigDecimal.TEN,3); agregar(sesion,tipo);
        String email=UUID.randomUUID()+"@test.com", clave=UUID.randomUUID().toString();
        var primera=comprar(sesion,clave,email); assertThat(primera.getResponse().getStatus()).isEqualTo(200);
        var datos=json.readTree(cuerpo(primera)); long orden=datos.at("/detalle/orden/id").asLong();
        assertThat(datos.at("/detalle/orden/comprador/id").isNull()).isTrue();
        assertThat(datos.at("/detalle/tickets").size()).isEqualTo(1);
        var segunda=comprar(sesion,clave,email); assertThat(segunda.getResponse().getStatus()).isEqualTo(200);
        assertThat(json.readTree(cuerpo(segunda)).at("/detalle/orden/id").asLong()).isEqualTo(orden);
        assertThat(eventos.consultarDisponibilidad(tipo).cupoDisponible()).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from usuarios.usuario where email=?",Integer.class,email)).isZero();
        assertThat(comprar(sesion,clave,"otro@test.com").getResponse().getStatus()).isEqualTo(409);
        String token=datos.get("token").asText();
        mvc.perform(get("/api/ventas/invitados/orden").header("X-Orden-Token",token)).andExpect(status().isOk())
            .andExpect(jsonPath("$.orden.id").value(orden));
        mvc.perform(get("/api/ventas/invitados/orden").header("X-Orden-Token",token+"x")).andExpect(status().isNotFound());
        mvc.perform(get("/api/ventas/ordenes/{id}",orden)).andExpect(status().isUnauthorized());
    }
    @Test void faltaDeCupoYPagoRechazadoNoEmitenOrden() throws Exception {
        var sesion=new MockHttpSession(); long tipo=tipo(new BigDecimal("10"),2); agregar(sesion,tipo);
        assertThat(comprar(sesion,UUID.randomUUID().toString(),"invitado@test.com","4000000000000002").getResponse().getStatus()).isEqualTo(402);
        assertThat(eventos.consultarDisponibilidad(tipo).cupoDisponible()).isEqualTo(2);
        var otro=new MockHttpSession(); long agotado=tipo(BigDecimal.ONE,1); agregar(otro,agotado);
        jdbc.update("update eventos.tipo_entrada set cupo_disponible=0 where id=?",agotado);
        assertThat(comprar(otro,UUID.randomUUID().toString(),"invitado@test.com").getResponse().getStatus()).isEqualTo(409);
    }
    @Test void compraInvitadaConPasarelaCaidaBloqueaUnSegundoCobroConLaMismaClave() throws Exception {
        var sesion=new MockHttpSession(); long tipo=tipo(BigDecimal.TEN,2); agregar(sesion,tipo);
        String clave=UUID.randomUUID().toString();
        assertThat(comprar(sesion,clave,"ambiguo@test.com","4000000000000119").getResponse().getStatus()).isEqualTo(503);
        assertThat(comprar(sesion,clave,"ambiguo@test.com","4000000000000119").getResponse().getStatus()).isEqualTo(409);
        assertThat(jdbc.queryForObject("select estado from ventas.intento_compra where clave=?",String.class,clave)).isEqualTo("PROCESANDO");
        assertThat(jdbc.queryForObject("select count(*) from ventas.orden o join ventas.intento_compra i on i.orden_id=o.id where i.clave=?",Integer.class,clave)).isZero();
    }
    @Test void reversionFallidaDevuelve503YElIntentoNoVuelveACobrar() throws Exception {
        var sesion=new MockHttpSession(); long tipo=tipo(BigDecimal.TEN,2); agregar(sesion,tipo);
        String clave=UUID.randomUUID().toString();
        org.mockito.Mockito.doAnswer(invocacion -> {
            jdbc.update("update eventos.tipo_entrada set cupo_disponible=0 where id=?",tipo);
            return new com.passly.pagos.dto.ResultadoDeCobroDTO(9876L,"ext-aprobado",
                com.passly.pagos.EstadoDePago.APROBADO,BigDecimal.TEN,"ARS",OffsetDateTime.now());
        }).when(pagos).cobrar(org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.doThrow(new com.passly.pagos.PasarelaDePagoNoDisponibleException(
            clave,new IllegalStateException("sin respuesta de reversión"))).when(pagos).revertir(9876L);
        try {
            var respuesta=comprar(sesion,clave,"reversion@test.com");
            assertThat(respuesta.getResponse().getStatus()).isEqualTo(503);
            assertThat(comprar(sesion,clave,"reversion@test.com").getResponse().getStatus()).isEqualTo(409);
            org.mockito.Mockito.verify(pagos,org.mockito.Mockito.times(1)).cobrar(org.mockito.ArgumentMatchers.any());
            assertThat(jdbc.queryForObject("select estado from ventas.intento_compra where clave=?",String.class,clave)).isEqualTo("PROCESANDO");
            assertThat(jdbc.queryForObject("select count(*) from ventas.intento_compra where clave=? and orden_id is not null",Integer.class,clave)).isZero();
        } finally {
            jdbc.update("update eventos.tipo_entrada set cupo_disponible=2 where id=?",tipo);
        }
    }
    @Test void recuperacionRotaAccesoYLimitaSolicitudes() throws Exception {
        var sesion=new MockHttpSession(); agregar(sesion,tipo(BigDecimal.ONE,2)); String email=UUID.randomUUID()+"@test.com";
        var compra=json.readTree(cuerpo(comprar(sesion,UUID.randomUUID().toString(),email)));
        long orden=compra.at("/detalle/orden/id").asLong(); String anterior=compra.get("token").asText();
        String ipPrueba="198.51.100."+(2+new java.util.Random().nextInt(250));
        for(int n=0;n<5;n++) mvc.perform(post("/api/ventas/invitados/recuperacion")
            .with(request -> { request.setRemoteAddr(ipPrueba); return request; }).contentType(APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("orden",orden,"email",email)))).andExpect(status().isAccepted());
        assertThat(jdbc.queryForObject("select count(*) from notificaciones.correo where destinatario=?",Integer.class,email)).isEqualTo(4);
        mvc.perform(get("/api/ventas/invitados/orden").header("X-Orden-Token",anterior)).andExpect(status().isNotFound());
        String texto=jdbc.queryForObject("select cuerpo from notificaciones.correo where destinatario=? order by id desc limit 1",String.class,email);
        String nuevo=texto.substring(texto.indexOf('#')+1,texto.indexOf(". El enlace"));
        mvc.perform(get("/api/ventas/invitados/orden").header("X-Orden-Token",nuevo)).andExpect(status().isOk());
    }
    @Test void transferenciaExigeSesionYConservaSeleccion() throws Exception {
        var sesion=new MockHttpSession(); agregar(sesion,tipo(BigDecimal.TEN,3)); var comprador=cuenta(Rol.COMPRADOR);
        mvc.perform(post("/api/ventas/invitados/transferencia").session(sesion)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/ventas/invitados/transferencia").session(sesion).with(user(actuar(comprador)).roles("COMPRADOR")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].cantidad").value(1));
        mvc.perform(get("/api/ventas/invitados/carrito").session(sesion)).andExpect(jsonPath("$.items.length()").value(0));
    }
    @Test void facturaInvitadaConservaIdentidadYEsIdempotente() {
        long orden=Math.abs(UUID.randomUUID().getMostSignificantBits() >>> 1);
        facturacion.registrarOrdenPagada(orden,null,BigDecimal.TEN,OffsetDateTime.now(),"Invitado", "invitado@test.com");
        facturacion.registrarOrdenPagada(orden,null,BigDecimal.TEN,OffsetDateTime.now(),"Otro nombre", "otro@test.com");
        assertThat(jdbc.queryForObject("select comprador_id from facturacion.factura where orden_id=?",Long.class,orden)).isNull();
        assertThat(jdbc.queryForObject("select comprador_nombre from facturacion.factura where orden_id=?",String.class,orden)).isEqualTo("Invitado");
        assertThat(jdbc.queryForObject("select comprador_email from facturacion.factura where orden_id=?",String.class,orden)).isEqualTo("invitado@test.com");
    }

    @Test void protegeCarritoContraOrigenAjeno() throws Exception {
        mvc.perform(post("/api/ventas/invitados/carrito/items").header("Origin","https://otro.test")
            .contentType(APPLICATION_JSON).content("{\"idTipoEntrada\":1,\"cantidad\":1}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/ventas/invitados/carrito/items").header("Sec-Fetch-Site","cross-site")
            .contentType(APPLICATION_JSON).content("{\"idTipoEntrada\":1,\"cantidad\":1}"))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("origen de la compra")));
    }
}
