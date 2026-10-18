package com.passly.facturacion.internal.negocio;

import com.passly.facturacion.FacturacionService;
import com.passly.facturacion.internal.datos.FacturaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
class FacturacionServiceImpl implements FacturacionService {
    private final FacturaRepository repository;
    private final ClienteAfip afip;
    FacturacionServiceImpl(FacturaRepository repository, ClienteAfip afip) {
        this.repository = repository;
        this.afip = afip;
    }
    @Transactional
    public void registrarOrdenPagada(Long idOrden, Long idComprador, BigDecimal total, OffsetDateTime cobradaEn) {
        if (idOrden == null || idOrden <= 0 || idComprador == null || idComprador <= 0
                || total == null || total.signum() < 0 || cobradaEn == null) {
            throw new IllegalArgumentException("Mensaje orden.pagada incompleto o invalido");
        }
        if (repository.insertarSiNoExiste(idOrden, idComprador, total, cobradaEn) == 1) {
            afip.solicitarFacturacion(idOrden);
        }
    }
}
