package com.passly.ventas.internal.negocio;
import com.passly.ventas.internal.datos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service
class IntentosCompra {
    private final IntentoCompraRepository repository;
    IntentosCompra(IntentoCompraRepository repository) { this.repository=repository; }
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public Long iniciar(String clave,String propietario,String huella) {
        if (repository.iniciar(clave,propietario,huella)==1) return null;
        var intento=repository.findById(clave).orElseThrow();
        if (!intento.propietario.equals(propietario)||!intento.huella.equals(huella))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"La clave corresponde a otro intento.");
        if (intento.ordenId!=null) return intento.ordenId;
        throw new ResponseStatusException(HttpStatus.CONFLICT,intento.estado.equals("FALLIDO")?
            "El intento falló. Revisá la compra antes de iniciar otro intento.":"El pago está en proceso o requiere conciliación. No vuelvas a pagar.");
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void fallar(String clave) { repository.findById(clave).ifPresent(i->i.estado="FALLIDO"); }
}
