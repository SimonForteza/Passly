package com.passly.productoras.internal.negocio;

import com.passly.productoras.*;
import com.passly.productoras.internal.datos.*;
import com.passly.notificaciones.CorreoService;
import com.passly.usuarios.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
import java.time.OffsetDateTime;
import java.security.*;
import java.nio.charset.StandardCharsets;

@Service
@Transactional
public class InvitacionService {
    private final InvitacionRepository invitaciones;
    private final ProductoraRepository productoras;
    private final UsuarioService usuarios;
    private final CorreoService correos;
    private final String origen;
    public InvitacionService(InvitacionRepository i,ProductoraRepository p,UsuarioService u,CorreoService c,
            @Value("${passly.web.origen}") String origen) {
        invitaciones=i; productoras=p; usuarios=u; correos=c; this.origen=origen;
    }
    public record Vista(Long id,String email,RolEnProductora rol,String estado,OffsetDateTime venceEn,String entrega) {}
    public record Publica(Long idProductora,String email,String productora,RolEnProductora rol,String estado,OffsetDateTime venceEn) {}
    public List<Vista> listar(Long productora,Long actor) {
        exigirDuenio(productora,actor);
        return invitaciones.findByProductoraIdOrderByIdDesc(productora).stream().map(this::vista).toList();
    }
    public Vista crear(Long productora,Long actor,String email,RolEnProductora rol) {
        var p=exigirDuenio(productora,actor);
        if (rol==null || rol==RolEnProductora.DUENIO) throw error(HttpStatus.BAD_REQUEST,"Elegí staff o validador.");
        email=email.trim().toLowerCase(Locale.ROOT);
        final String destinatario=email;
        if (p.getMiembros().stream().anyMatch(m->usuarios.consultarUsuario(m.getUsuarioId()).email().equalsIgnoreCase(destinatario)))
            throw error(HttpStatus.CONFLICT,"Esa persona ya pertenece a la productora.");
        if (invitaciones.findByProductoraIdOrderByIdDesc(productora).stream()
                .anyMatch(i->i.email.equals(destinatario)&&estado(i).equals("PENDIENTE")))
            throw error(HttpStatus.CONFLICT,"Ya hay una invitación pendiente para ese email.");
        var i=new Invitacion(productora,email,rol); enviar(i,p.getNombreComercial());
        return vista(invitaciones.save(i));
    }
    public Vista reenviar(Long productora,Long id,Long actor) {
        var p=exigirDuenio(productora,actor); var i=buscar(productora,id);
        if (i.estado.equals("ACEPTADA")||i.estado.equals("REVOCADA")) throw error(HttpStatus.CONFLICT,"La invitación ya no se puede reenviar.");
        enviar(i,p.getNombreComercial()); return vista(i);
    }
    public void revocar(Long productora,Long id,Long actor) {
        exigirDuenio(productora,actor); var i=buscar(productora,id);
        if (i.estado.equals("ACEPTADA")) throw error(HttpStatus.CONFLICT,"La invitación ya fue aceptada.");
        i.estado="REVOCADA";
    }
    @Transactional(readOnly=true)
    public Publica consultar(String token) {
        var i=invitaciones.findByTokenHash(hash(token)).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Invitación no encontrada."));
        return new Publica(i.productoraId,i.email,productoras.findById(i.productoraId).orElseThrow().getNombreComercial(),i.rol,estado(i),i.venceEn);
    }
    public Long aceptar(String token,Long actor) {
        // Mismo orden de locks que crear/reenviar: productora -> invitación -> usuario.
        var referencia=invitaciones.findByTokenHash(hash(token)).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Invitación no encontrada."));
        var p=productoras.bloquear(referencia.productoraId).orElseThrow();
        var i=invitaciones.bloquearPorToken(hash(token)).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Invitación no encontrada."));
        var usuario=usuarios.consultarUsuario(actor);
        if (!usuario.email().equalsIgnoreCase(i.email)) throw error(HttpStatus.FORBIDDEN,"Ingresá con el email que recibió la invitación.");
        if (i.estado.equals("ACEPTADA")) return i.productoraId;
        if (!estado(i).equals("PENDIENTE")) throw error(HttpStatus.GONE,"La invitación venció o fue revocada.");
        if (!p.tieneMiembro(actor)) {
            usuarios.habilitarCapacidad(actor,i.rol==RolEnProductora.STAFF?Rol.ORGANIZADOR:Rol.VALIDADOR);
            p.incorporar(new Miembro(actor,i.rol));
        }
        i.estado="ACEPTADA"; return i.productoraId;
    }
    private Productora exigirDuenio(Long id,Long actor) {
        var p=productoras.bloquear(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Productora no encontrada."));
        if (!p.puedeGestionarMiembros(actor)) throw error(HttpStatus.FORBIDDEN,"Solo el dueño puede gestionar invitaciones.");
        return p;
    }
    private Invitacion buscar(Long productora,Long id) {
        var i=invitaciones.bloquear(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Invitación no encontrada."));
        if (!i.productoraId.equals(productora)) throw error(HttpStatus.NOT_FOUND,"Invitación no encontrada.");
        return i;
    }
    private String estado(Invitacion i) { return i.estado.equals("PENDIENTE")&&i.venceEn.isBefore(OffsetDateTime.now())?"VENCIDA":i.estado; }
    private Vista vista(Invitacion i) { return new Vista(i.id,i.email,i.rol,estado(i),i.venceEn,correos.estado(i.correoId)); }
    private void enviar(Invitacion i,String nombre) {
        byte[] bytes=new byte[32]; new SecureRandom().nextBytes(bytes);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        i.tokenHash=hash(token); i.venceEn=OffsetDateTime.now().plusDays(7); i.estado="PENDIENTE";
        i.correoId=correos.encolar(i.email,"Invitación a "+nombre,
            "Te invitaron a "+nombre+" como "+i.rol+". Aceptá en los próximos siete días: "+origen+"/invitaciones#"+token);
    }
    private static String hash(String token) {
        if (token==null || token.length()>200) throw error(HttpStatus.NOT_FOUND,"Invitación no encontrada.");
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static ResponseStatusException error(HttpStatus status,String texto) { return new ResponseStatusException(status,texto); }
}
