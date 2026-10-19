package com.passly.productoras.internal.web;
import com.passly.productoras.RolEnProductora;
import com.passly.productoras.internal.negocio.InvitacionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.List;
@RestController
class InvitacionController {
    private final InvitacionService service;
    InvitacionController(InvitacionService service) { this.service=service; }
    record Nueva(@NotBlank @Email @Size(max=254) String email,@NotNull RolEnProductora rolEnProductora) {}
    @GetMapping("/api/productoras/{id}/invitaciones")
    List<InvitacionService.Vista> listar(@PathVariable Long id,Authentication a) { return service.listar(id,actor(a)); }
    @PostMapping("/api/productoras/{id}/invitaciones")
    InvitacionService.Vista crear(@PathVariable Long id,Authentication a,@Valid @RequestBody Nueva r) { return service.crear(id,actor(a),r.email(),r.rolEnProductora()); }
    @PostMapping("/api/productoras/{id}/invitaciones/{invitacion}/reenvio")
    InvitacionService.Vista reenviar(@PathVariable Long id,@PathVariable Long invitacion,Authentication a) { return service.reenviar(id,invitacion,actor(a)); }
    @DeleteMapping("/api/productoras/{id}/invitaciones/{invitacion}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    void revocar(@PathVariable Long id,@PathVariable Long invitacion,Authentication a) { service.revocar(id,invitacion,actor(a)); }
    @GetMapping("/api/invitaciones")
    InvitacionService.Publica consultar(@RequestHeader("X-Invitacion-Token") String token) { return service.consultar(token); }
    @PostMapping("/api/invitaciones/aceptacion")
    Long aceptar(@RequestHeader("X-Invitacion-Token") String token,Authentication a) { return service.aceptar(token,actor(a)); }
    private Long actor(Authentication a) { return Long.valueOf(a.getName()); }
}
