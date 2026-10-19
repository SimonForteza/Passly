package com.passly.ventas.internal.negocio;
import com.passly.eventos.EventoService;
import com.passly.ventas.VentaService;
import com.passly.ventas.dto.*;
import com.passly.ventas.internal.datos.OrdenRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
@Service
public class CompraInvitadoService {
    private static final Long INVITADO=-1L;
    private final CarritoInvitado sesion;
    private final EventoService eventos;
    private final VentaService ventas;
    private final PasarelaDePago pasarela;
    private final ConfirmacionDeCompra confirmacion;
    private final IntentosCompra intentos;
    private final ConsultaInvitado consultas;
    public CompraInvitadoService(CarritoInvitado s,EventoService e,VentaService v,PasarelaDePago p,
            ConfirmacionDeCompra c,IntentosCompra i,ConsultaInvitado consultas) {
        sesion=s; eventos=e; ventas=v; pasarela=p; confirmacion=c; intentos=i; this.consultas=consultas;
    }
    public CarritoDTO ver() { synchronized(sesion.candado()) { return sesion.carrito().verComo(INVITADO); } }
    public CarritoDTO agregar(AgregarItemRequest r) {
        synchronized(sesion.candado()) {
            var d=eventos.consultarDisponibilidad(r.idTipoEntrada());
            eventos.consultarEventoPublico(d.idEvento());
            if (sesion.carrito().verComo(INVITADO).expirado()) sesion.reiniciar();
            sesion.carrito().agregarItem(INVITADO,d.idTipoEntrada(),d.idEvento(),d.nombreTipoEntrada(),d.precio(),r.cantidad());
            return sesion.carrito().verComo(INVITADO);
        }
    }
    public void vaciar() { synchronized(sesion.candado()) { sesion.reiniciar(); } }
    public CarritoDTO transferir(Long actor) {
        synchronized(sesion.candado()) {
            var items=sesion.carrito().verComo(INVITADO).items();
            ventas.agregarItems(items.stream().map(item -> new AgregarItemRequest(item.idTipoEntrada(),item.cantidad())).toList(),actor);
            sesion.reiniciar(); return ventas.verCarrito(actor);
        }
    }
    public record Confirmada(ConsultaInvitado.Detalle detalle,String token) {}
    public Confirmada confirmar(String clave,String nombre,String email,DatosDeTarjetaDTO tarjeta) {
        try { UUID.fromString(clave); } catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La clave de intento debe ser un UUID."); }
        synchronized(sesion.candado()) {
            String normal=email.trim().toLowerCase(Locale.ROOT);
            String datos=SeguridadEnlaces.hash(nombre.trim()+"\n"+normal);
            String anterior=sesion.huellas().get(clave);
            if (anterior!=null && !anterior.startsWith(datos+":")) throw new ResponseStatusException(HttpStatus.CONFLICT,"El intento tiene otros datos.");
            // Una respuesta perdida puede recuperarse aun después de vaciar el carrito.
            if (anterior!=null) {
                intentos.iniciar(clave,SeguridadEnlaces.hash(sesion.propietario()),anterior.substring(anterior.indexOf(':')+1));
                String token=sesion.tokenPara(clave);
                return new Confirmada(consultas.consultar(token),token);
            }
            var items=sesion.carrito().itemsParaConfirmar(INVITADO);
            OffsetDateTime ultimo=OffsetDateTime.MIN;
            for (var item:items) {
                var d=eventos.consultarDisponibilidad(item.idTipoEntrada());
                var evento=eventos.consultarEventoPublico(d.idEvento());
                if (!d.hayDisponibilidad()||d.cupoDisponible()<item.cantidad()) throw new ResponseStatusException(HttpStatus.CONFLICT,"No hay cupo para tu selección.");
                if (evento.fechaHora().isAfter(ultimo)) ultimo=evento.fechaHora();
            }
            String huella=datos+":"+SeguridadEnlaces.hash(items.toString());
            // Persistir un hash de tamaño fijo, conservando los datos para validar reintentos locales.
            String huellaDb=SeguridadEnlaces.hash(huella);
            intentos.iniciar(clave,SeguridadEnlaces.hash(sesion.propietario()),huellaDb);
            sesion.huellas().put(clave,datos+":"+huellaDb);
            String token=sesion.tokenPara(clave);
            BigDecimal total=sesion.carrito().total();
            ComprobanteDeCobroDTO cobro;
            try { cobro=pasarela.cobrar(total,clave,tarjeta); }
            catch (com.passly.ventas.PagoRechazadoException e) { intentos.fallar(clave); throw e; }
            // Otros errores del proveedor son ambiguos: el intento queda PROCESANDO, sin recobro automático.
            try {
                confirmacion.confirmarInvitado(nombre.trim(),normal,token,clave,ultimo.plusDays(30),items,total,cobro.id(),cobro.autorizadoEn());
            } catch (RuntimeException e) {
                try {
                    pasarela.revertir(cobro.id());
                    intentos.fallar(clave);
                } catch (RuntimeException compensacion) {
                    e.addSuppressed(compensacion);
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "El pago necesita revisión. No vuelvas a intentar hasta recibir ayuda.",e);
                }
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El cobro fue revertido. Revisá disponibilidad e iniciá un nuevo intento.",e);
            }
            sesion.reiniciar();
            return new Confirmada(consultas.consultar(token),token);
        }
    }
}
