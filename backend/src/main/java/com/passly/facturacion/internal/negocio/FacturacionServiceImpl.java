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
    public void registrarOrdenPagada(Long idOrden,Long idComprador,BigDecimal total,OffsetDateTime cobradaEn) {
        registrarOrdenPagada(idOrden,idComprador,total,cobradaEn,null,null);
    }
    @Override
    @Transactional
    public void registrarOrdenPagada(Long idOrden,Long idComprador,BigDecimal total,OffsetDateTime cobradaEn,
            String compradorNombre,String compradorEmail) {
        if (idOrden == null || idOrden <= 0 || (idComprador != null && idComprador <= 0)
                || total == null || total.signum() < 0 || cobradaEn == null) {
            throw new IllegalArgumentException("Mensaje orden.pagada incompleto o invalido");
        }
        boolean insertada= idComprador==null && compradorNombre!=null && compradorEmail!=null
                ? repository.insertarInvitadoSiNoExiste(idOrden,null,compradorNombre,compradorEmail,total,cobradaEn)==1
                : repository.insertarSiNoExiste(idOrden,idComprador,total,cobradaEn)==1;
        if (insertada) {
            afip.solicitarFacturacion(idOrden);
        }
    }
}
