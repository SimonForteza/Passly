package com.passly.seguridad.internal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;

@Component
class OrigenInvitadoFilter extends OncePerRequestFilter {
    private final String origen;
    private final ObjectMapper mapper;
    OrigenInvitadoFilter(@Value("${passly.web.origen}") String origen, ObjectMapper mapper) {
        this.origen = origen;
        this.mapper = mapper;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ruta = request.getRequestURI();
        if (ruta.startsWith("/api/ventas/invitados") || ruta.startsWith("/api/invitaciones")) {
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Referrer-Policy", "no-referrer");
        }
        String recibido = request.getHeader("Origin");
        boolean muta = !request.getMethod().equals("GET") && !request.getMethod().equals("OPTIONS");
        if (ruta.startsWith("/api/ventas/invitados") && muta
                && ((recibido != null && !recibido.equals(origen))
                || "cross-site".equals(request.getHeader("Sec-Fetch-Site")))) {
            // Responder acá evita que el redispatch a /error transforme el 403 en un 401.
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            mapper.writeValue(response.getWriter(), ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                    "No pudimos verificar el origen de la compra. Volvé a abrir Passly desde su dirección habitual."));
            return;
        }
        chain.doFilter(request, response);
    }
}
