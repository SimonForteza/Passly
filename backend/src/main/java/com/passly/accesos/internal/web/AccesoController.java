package com.passly.accesos.internal.web;

import com.passly.accesos.AccesoService;
import com.passly.accesos.dto.ValidacionDTO;
import com.passly.accesos.dto.ValidarEntradaRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
class AccesoController {
    private final AccesoService accesos;
    AccesoController(AccesoService accesos) { this.accesos = accesos; }

    @PostMapping("/api/accesos/validaciones")
    @PreAuthorize("hasAnyRole('VALIDADOR','ORGANIZADOR','ADMIN')")
    ValidacionDTO validar(@Valid @RequestBody ValidarEntradaRequest solicitud, Authentication actor) {
        return accesos.validarEntrada(solicitud, Long.valueOf(actor.getName()));
    }
}
