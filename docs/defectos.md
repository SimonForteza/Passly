# Registro de defectos

Cada defecto que encuentra el equipo se registra acá apenas se detecta, aunque se corrija en el
momento. Un defecto registrado y corregido por el equipo vale más que uno que encuentra la
cátedra.

**Severidad:** crítica (se pierde plata, datos o se rompe una regla de seguridad), alta (un
recorrido de la visión no funciona), media (funciona con un rodeo), baja (cosmético).

| Id | Descripción | Severidad | Cómo se detectó | Estado | Corrección |
|---|---|---|---|---|---|
| DEF-001 | Ventas permitía comprar entradas de un evento en **borrador** o **cancelado**. Adivinando el id de un tipo de entrada se podía agregar al carrito (con cuenta o como invitado, en el cancelado) y, con cuenta, confirmar cobraba la tarjeta antes de que Eventos rechazara el descuento de cupo, para revertir el cobro después. | Crítica: es un IDOR/BFLA sobre una fiesta no anunciada y un cobro que no debía ocurrir | Revisión de código durante PAS-25 (link compartible); quedó anotado en CLAUDE.md §9 como hallazgo fuera de alcance y se cargó como PAS-37 | Corregido | PAS-37: Ventas rechaza con **409** al agregar al carrito (con cuenta y como invitado) y **antes de cobrar** al confirmar. Eventos ya informaba `hayDisponibilidad=false` fuera de `PUBLICADO` desde PAS-35, y `descontarCupo` sigue siendo la última barrera dentro de la transacción. Tests: `ConfirmarCompraTest` (borrador, cancelado, cancelado con carrito abierto sin cobro) y `AltasInvitadosIntegrationTest.invitadoNoAgregaEntradasDeEventosFueraDeVenta`. Demo: `docs/http/14-compra-fuera-de-venta.http`. |

## DEF-001: decisiones

- **409 y no 404 en el flujo con cuenta.** El tipo de entrada existe y su disponibilidad ya es
  pública (`GET /api/tipos-entrada/{id}/disponibilidad`); lo que se rechaza es la operación en el
  estado actual. Para no filtrar información, el mensaje es genérico ("no está disponible para la
  venta") y no distingue borrador, cancelado ni agotado.
- **El invitado conserva el 404 para el borrador.** El flujo invitado ya consultaba
  `consultarEventoPublico`, que oculta los borradores (PAS-25); se mantuvo. Para el cancelado, que
  `consultarEventoPublico` deja pasar a propósito, ahora responde 409.
- **Validar antes de cobrar, y no solo en la transacción.** El chequeo nuevo en
  `VentaServiceImpl.confirmarCompra` evita cobrar y revertir. Si el evento se cancela justo entre
  el chequeo y el commit, `descontarCupo` lo rechaza dentro de la transacción y el cobro se
  revierte como cualquier otra falla de confirmación.
- **Sin joins ni dependencias nuevas.** Ventas no conoce el estado del evento: usa
  `hayDisponibilidad`, que Eventos calcula con `admiteVentas()` del patrón State.
