package com.passly.ventas.internal.negocio;
import com.passly.ventas.internal.datos.*;
import com.passly.ventas.dto.*;
import com.passly.tickets.TicketService;
import com.passly.tickets.dto.TicketDTO;
import com.passly.notificaciones.CorreoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.OffsetDateTime;
import java.util.*;
@Service
@Transactional
public class ConsultaInvitado {
    private final OrdenRepository ordenes;
    private final TicketService tickets;
    private final VentaMapper mapper;
    private final CorreoService correos;
    private final String origen;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    public ConsultaInvitado(OrdenRepository o,TicketService t,VentaMapper m,CorreoService c,
            org.springframework.jdbc.core.JdbcTemplate jdbc,@Value("${passly.web.origen}") String origen) {
        ordenes=o; tickets=t; mapper=m; correos=c; this.origen=origen; this.jdbc=jdbc;
    }
    public record Detalle(OrdenDTO orden,List<TicketDTO> tickets) {}
    public Detalle consultar(String token) {
        if (token==null||token.length()!=43) throw noEncontrada();
        var orden=ordenes.findByAccesoHash(SeguridadEnlaces.hash(token)).orElseThrow(ConsultaInvitado::noEncontrada);
        if (orden.getCompradorId()!=null || orden.getAccesoVenceEn().isBefore(OffsetDateTime.now())) throw noEncontrada();
        return detalle(orden);
    }
    Detalle detalle(Orden orden) {
        return new Detalle(mapper.aDTO(orden,new CompradorDeOrdenDTO(null,orden.getCompradorEmail(),orden.getCompradorNombre())),
            tickets.listarTicketsDeOrden(orden.getId(),null));
    }
    public void recuperar(Long id,String email,String direccion) {
        // Límite persistente por IP y por combinación orden/email: misma respuesta para todos.
        String normal=email.trim().toLowerCase(Locale.ROOT);
        if (!permitir("ip:"+direccion,10) || !permitir(id+":"+normal,3)) return;
        var encontrada=ordenes.bloquear(id);
        if (encontrada.isEmpty()) return;
        var orden=encontrada.get();
        if (orden.getCompradorId()!=null || !normal.equalsIgnoreCase(orden.getCompradorEmail())
                || orden.getAccesoVenceEn().isBefore(OffsetDateTime.now())) return;
        String token=SeguridadEnlaces.token(); orden.renovarAcceso(SeguridadEnlaces.hash(token));
        correos.encolar(orden.getCompradorEmail(),"Nuevo enlace de tu orden "+id,
                "Consultá tus entradas en "+origen+"/orden-invitado#"+token+". El enlace anterior dejó de funcionar.");
    }
    private boolean permitir(String valor,int limite) {
        Integer cantidad=jdbc.queryForObject("""
            insert into ventas.limite_recuperacion (clave,ventana,cantidad) values (?,current_timestamp,1)
            on conflict (clave) do update set
              cantidad=case when ventas.limite_recuperacion.ventana < current_timestamp-interval '1 hour' then 1 else ventas.limite_recuperacion.cantidad+1 end,
              ventana=case when ventas.limite_recuperacion.ventana < current_timestamp-interval '1 hour' then current_timestamp else ventas.limite_recuperacion.ventana end
            returning cantidad
            """,Integer.class,SeguridadEnlaces.hash(valor));
        return cantidad!=null && cantidad<=limite;
    }
    private static ResponseStatusException noEncontrada() { return new ResponseStatusException(HttpStatus.NOT_FOUND,"El enlace no es válido o venció."); }
}
