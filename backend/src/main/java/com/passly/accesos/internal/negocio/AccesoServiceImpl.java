package com.passly.accesos.internal.negocio;

import com.passly.accesos.AccesoService;
import com.passly.accesos.dto.ValidacionDTO;
import com.passly.accesos.dto.ValidacionDTO.Resultado;
import com.passly.accesos.dto.ValidarEntradaRequest;
import com.passly.accesos.internal.datos.Validacion;
import com.passly.accesos.internal.datos.ValidacionRepository;
import com.passly.eventos.EventoService;
import com.passly.eventos.EstadoEvento;
import com.passly.productoras.ProductoraService;
import com.passly.productoras.NoEsMiembroDeLaProductoraException;
import com.passly.tickets.TicketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AccesoServiceImpl implements AccesoService {
    private final EventoService eventos;
    private final ProductoraService productoras;
    private final TicketService tickets;
    private final ValidacionRepository validaciones;
    AccesoServiceImpl(EventoService eventos, ProductoraService productoras,
                     TicketService tickets, ValidacionRepository validaciones) {
        this.eventos = eventos;
        this.productoras = productoras;
        this.tickets = tickets;
        this.validaciones = validaciones;
    }
    @Transactional
    public ValidacionDTO validarEntrada(ValidarEntradaRequest solicitud, Long idValidador) {
        var evento = eventos.consultarEventoPublico(solicitud.idEvento());
        if (!productoras.puedeValidarEntradas(evento.organizador().id(), idValidador)) {
            throw new NoEsMiembroDeLaProductoraException(evento.organizador().id(), idValidador);
        }
        if (evento.estado() != EstadoEvento.PUBLICADO) {
            validaciones.save(new Validacion(null, evento.id(), idValidador, Resultado.INVALIDO));
            return new ValidacionDTO(Resultado.INVALIDO, null, "Este evento no admite ingresos.");
        }
        var uso = tickets.validarYUsar(solicitud.contenidoQr(), evento.id());
        var resultado = Resultado.valueOf(uso.resultado().name());
        validaciones.save(new Validacion(uso.idTicket(), evento.id(), idValidador, resultado));
        var ticket = resultado == Resultado.VALIDO || resultado == Resultado.YA_USADO
                ? new ValidacionDTO.TicketValidado(uso.idTicket(), uso.idEvento(), uso.nombreTipoEntrada(), uso.usadoEn())
                : null;
        String mensaje = switch (resultado) {
            case VALIDO -> "Entrada valida. Puede ingresar.";
            case YA_USADO -> "Esta entrada ya fue utilizada.";
            case OTRO_EVENTO -> "Esta entrada pertenece a otro evento.";
            case INVALIDO -> "El QR no corresponde a una entrada valida.";
        };
        return new ValidacionDTO(resultado, ticket, mensaje);
    }
}
