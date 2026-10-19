package com.passly.notificaciones;

public interface CorreoService {
    Long encolar(String destinatario, String asunto, String cuerpo);
    String estado(Long id);
}
