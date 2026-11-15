# ADR-009: GRASP Pure Fabrication para el límite transaccional de la compra

- Estado: Aceptado (implementado en PAS-8)
- Fecha: 2026-10-26 (formaliza la decisión de PAS-8)
- Responsables: Equipo Passly
- Alcance: `ventas.internal.negocio.ConfirmacionDeCompra`
- Tipo de patrón: **GRASP** (Pure Fabrication, con Indirection y Protected Variations)

## Contexto

Confirmar una compra tiene dos partes con necesidades opuestas:

1. **Cobrar** contra la pasarela: externo, lento, no rollbackeable → tiene que quedar **fuera**
   de cualquier transacción de base.
2. **Descontar cupo → registrar la orden → emitir tickets**: tiene que ser **una sola**
   transacción declarativa (`@Transactional`), en varios esquemas de la misma base.

Spring aplica `@Transactional` con un proxy: si las dos partes vivieran en la misma clase y una
llamara a la otra con `this.metodo()`, la llamada no pasaría por el proxy y la transacción no se
abriría. Si en cambio la clase entera fuera transaccional, la transacción se abriría **antes** de
cobrar.

## Fuerzas y restricciones

- Ningún concepto del dominio ("Orden", "Evento", "Carrito") es el experto natural en "límite
  transaccional de varios agregados de distintos módulos".
- `Orden` no puede descontar cupo de Eventos ni emitir Tickets: son otros módulos.
- La consigna exige transacciones declarativas en un flujo crítico multipaso.

## Alternativas consideradas

### Information Expert: poner la lógica en la entidad `Orden`

`Orden` no tiene acceso a `EventoService` ni a `TicketService` (y no debería: una entidad JPA no
recibe servicios inyectados). Descartada.

### `TransactionTemplate` programático dentro de `VentaServiceImpl`

Funciona, pero cambia transacción **declarativa** por programática (justo lo que se evalúa) y
mezcla cobro, compensación y transacción en un método.

### Auto-inyección (`@Lazy VentaServiceImpl self`)

Esquiva el problema del proxy con un truco poco legible y fácil de romper.

### Pure Fabrication (elegida)

Una clase **inventada**, sin correlato en el dominio, cuya única responsabilidad es ser el límite
transaccional.

## Decisión

Crear `ConfirmacionDeCompra` (bean package-private, `@Transactional` en sus métodos):
descuenta cupo vía `EventoService.descontarCupo` (se une por propagación `REQUIRED`), persiste la
`Orden`, emite tickets con `TicketService` y publica `OrdenPagada`. `VentaServiceImpl` (sin
`@Transactional` de clase) cobra con `PasarelaDePago`, llama a `ConfirmacionDeCompra` (salto
entre beans → pasa por el proxy) y, si falla, compensa.

Es además **Indirection** (Ventas no orquesta la transacción a mano) y **Protected Variations**
(si cambia cómo se persiste o se emite, cambia esta clase y no el Facade).

## Consecuencias positivas

- Un solo commit para `eventos.tipo_entrada`, `ventas.orden`, `ventas.item_orden` y
  `tickets.ticket`; rollback total probado (`ConfirmarCompraTest`).
- Alta cohesión: la clase hace una sola cosa.
- Reutilizada por la compra como invitado (`confirmarInvitado`).

## Consecuencias negativas

- Una clase más que hay que explicar ("¿por qué no está en el servicio?"): el Javadoc lo explica.
- La compensación del cobro sigue en el llamador y hoy está **duplicada** entre
  `VentaServiceImpl` y `CompraInvitadoService` — ver la auditoría SOLID (docs/patrones.md §4),
  que propone extraerla a otra Pure Fabrication (`CobroConCompensacion`).

## Evidencia en el repositorio

- `ventas/internal/negocio/ConfirmacionDeCompra.java` (Javadoc con el motivo del proxy).
- `ventas/internal/negocio/VentaServiceImpl.java` (sin `@Transactional` de clase).
- `backend/src/test/java/com/passly/ventas/ConfirmarCompraTest.java`.

## Criterio de revisión

Revisar si la compra pasa a ser distribuida (saga) o si se extrae `ServicioDeInventario`
(CLAUDE.md §10).
