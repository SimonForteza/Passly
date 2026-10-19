package com.passly.accesos;

import com.passly.accesos.dto.ValidacionDTO;
import com.passly.accesos.dto.ValidarEntradaRequest;

/**
 * Valida una entrada y audita el intento dentro de la misma transaccion del consumo.
 * La precarga via topico ticket.emitido se incorpora en la Obligatoria 2; hoy consulta Tickets.
 */
public interface AccesoService {
    ValidacionDTO validarEntrada(ValidarEntradaRequest solicitud, Long idValidador);
}
