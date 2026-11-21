package com.passly.eventos.internal.web;

import com.passly.eventos.ArchivoDePortadaInvalidoException;
import com.passly.eventos.EventoService;
import com.passly.eventos.dto.EventoDTO;
import com.passly.eventos.dto.PortadaDTO;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Portada de un evento (PAS-47): subir, quitar y servir la imagen.
 *
 * <p>Subir y quitar llevan la misma autorizacion que editar un evento: rol {@code ORGANIZADOR}
 * aca, y membresia sobre la productora en el servicio. Leer es publico, porque la cartelera la
 * muestra con un {@code <img>}, que no manda el token.
 *
 * <p><b>Cache con ETag.</b> La imagen se sirve con un ETag que cambia en cada subida. Si el
 * navegador ya la tiene, manda {@code If-None-Match} y recibe <b>304</b> sin bytes. Ademas, la URL
 * que viaja en {@code EventoDTO.portadaUrl} lleva {@code ?v=}, asi que una portada nueva es una
 * URL nueva y nunca se muestra la vieja.
 */
@RestController
class PortadaController {

    private final EventoService eventoService;

    PortadaController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    /** Multipart con el campo {@code archivo}. El tipo lo decide el servicio leyendo los bytes. */
    @PutMapping(path = "/api/eventos/{id}/portada", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ORGANIZADOR','ADMIN')")
    EventoDTO subirPortada(
            @PathVariable("id") Long id,
            @RequestParam("archivo") MultipartFile archivo,
            Authentication authentication
    ) {
        return eventoService.cambiarPortada(id, leer(archivo), idDelActuante(authentication));
    }

    @DeleteMapping("/api/eventos/{id}/portada")
    @PreAuthorize("hasAnyRole('ORGANIZADOR','ADMIN')")
    EventoDTO quitarPortada(@PathVariable("id") Long id, Authentication authentication) {
        return eventoService.quitarPortada(id, idDelActuante(authentication));
    }

    @GetMapping("/api/eventos/{id}/portada")
    ResponseEntity<byte[]> verPortada(
            @PathVariable("id") Long id,
            Authentication authentication,
            WebRequest request
    ) {
        PortadaDTO portada = EventoController.esAnonimo(authentication)
                ? eventoService.obtenerPortadaPublica(id)
                : eventoService.obtenerPortada(id, idDelActuante(authentication));

        String etag = "\"portada-" + id + "-" + portada.version() + "\"";
        // La de un borrador solo la ve quien la gestiona: no se cachea en intermediarios.
        CacheControl cache = portada.publica()
                ? CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic()
                : CacheControl.noCache().cachePrivate();

        if (request.checkNotModified(etag)) {
            // checkNotModified ya marco la respuesta como 304 con el ETag; no van bytes.
            return ResponseEntity.status(304).eTag(etag).cacheControl(cache).build();
        }
        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(cache)
                .contentType(MediaType.parseMediaType(portada.tipoContenido()))
                .body(portada.contenido());
    }

    private static byte[] leer(MultipartFile archivo) {
        try {
            return archivo.getBytes();
        } catch (IOException e) {
            throw new ArchivoDePortadaInvalidoException("No se pudo leer el archivo subido");
        }
    }

    private static Long idDelActuante(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
