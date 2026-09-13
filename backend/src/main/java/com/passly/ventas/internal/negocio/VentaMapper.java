package com.passly.ventas.internal.negocio;

import com.passly.ventas.dto.CompradorDeOrdenDTO;
import com.passly.ventas.dto.ItemDeOrdenDTO;
import com.passly.ventas.dto.OrdenDTO;
import com.passly.ventas.internal.datos.ItemOrden;
import com.passly.ventas.internal.datos.Orden;
import org.springframework.stereotype.Component;

/**
 * Mapeo manual entidad -> DTO, mismo patron que {@code EventoMapper}: sin estado, sin
 * dependencias inyectadas, y el dato externo ({@code CompradorDeOrdenDTO}) llega ya resuelto por
 * quien llama, para que la decision de como resolverlo (una consulta por orden o una sola para
 * un lote) quede a la vista en un solo lugar.
 */
@Component
class VentaMapper {

    OrdenDTO aDTO(Orden orden, CompradorDeOrdenDTO comprador) {
        return new OrdenDTO(
                orden.getId(),
                comprador,
                orden.getItems().stream().map(this::aDTO).toList(),
                orden.getTotal(),
                orden.getCreadaEn()
        );
    }

    ItemDeOrdenDTO aDTO(ItemOrden item) {
        return new ItemDeOrdenDTO(
                item.getTipoEntradaId(),
                item.getEventoId(),
                item.getNombreTipoEntrada(),
                item.getPrecioUnitario(),
                item.getCantidad()
        );
    }
}
