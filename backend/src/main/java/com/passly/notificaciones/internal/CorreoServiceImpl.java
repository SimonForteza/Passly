package com.passly.notificaciones.internal;
import com.passly.notificaciones.CorreoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional
class CorreoServiceImpl implements CorreoService {
    private final CorreoRepository repository;
    CorreoServiceImpl(CorreoRepository repository) { this.repository=repository; }
    public Long encolar(String destinatario,String asunto,String cuerpo) {
        return repository.save(new Correo(destinatario,asunto,cuerpo)).id;
    }
    @Transactional(readOnly=true)
    public String estado(Long id) { return repository.findById(id).map(c->c.estado).orElse("PENDIENTE"); }
}
