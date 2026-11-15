# ADR-003: Facade para la compra en ServicioDeVentas

- Estado: Aceptado
- Fecha: 2026-11-15
- Responsables: Equipo Passly
- Alcance: Componente `ventas`

## Contexto

Comprar una entrada atraviesa cinco componentes: Eventos (precio, disponibilidad y descuento de cupo), Usuarios (datos del comprador), Pagos (cobro y reversion), Tickets (un QR firmado por unidad) y Notificaciones (correo de la compra como invitado). Ademas, Facturacion recibe la orden por la cola `orden.pagada` despues del commit (PAS-32).

Quien inicia la compra, sea la app web o el controller REST, no deberia conocer ese orden de pasos ni sus reglas: primero se cobra fuera de la transaccion, despues se descuenta cupo, se registra la orden y se emiten los tickets en una sola transaccion, y si eso falla se revierte el cobro. Si cada cliente tuviera que coordinar la compra, la regla mas delicada del sistema quedaria repartida entre varios lugares.

## Fuerzas y restricciones

- El flujo critico tiene que ser una sola operacion para el cliente, con un solo resultado: orden confirmada o error.
- El cobro no puede quedar dentro de la transaccion: un externo lento bloquearia filas y su efecto no se puede deshacer con rollback (ADR-001, CLAUDE.md §2).
- El descuento de cupo, la orden y los tickets si tienen que confirmarse o revertirse juntos.
- Las fronteras de Spring Modulith obligan a que cada dependencia de Ventas este declarada en su `package-info.java`.

## Alternativas consideradas

### Que el cliente coordine los componentes

El frontend o el controller llamarian a `EventoService`, `PagoService` y `TicketService` por separado. Descartada: la transaccion quedaria en manos del cliente, que no puede abrirla. Cada cliente nuevo (la app movil, los datos de demo) repetiria la secuencia y la compensacion del cobro.

### Orquestador o saga aparte

Un componente "Compras" separado de Ventas, o una saga con eventos y compensaciones. Descartada: en un monolito modular con una sola base (ADR-001 y ADR-002) la transaccion local ya resuelve la consistencia. Una saga sumaria estados intermedios y compensaciones sin necesitarlas.

### Facade en ServicioDeVentas

Una interfaz de negocio con operaciones de alto nivel (`agregarItem`, `verCarrito`, `confirmarCompra`, `consultarOrden`, `listarOrdenesDeComprador`) que esconde la coordinacion con los demas componentes.

## Decision

`com.passly.ventas.VentaService` es el **Facade** de la compra. Su implementacion, `ventas.internal.negocio.VentaServiceImpl` (package-private), es la unica que conoce el orden de los pasos:

1. Lee el carrito sin vaciarlo (`CarritoDeCompra.itemsParaConfirmar`).
2. Cobra con el Port `PasarelaDePago` (ADR-004), **fuera** de toda transaccion.
3. Llama a `ConfirmacionDeCompra.confirmar`, el limite `@Transactional`: `EventoService.descontarCupo` (se une a la misma transaccion por propagacion `REQUIRED`), `OrdenRepository.saveAndFlush`, `TicketService.emitirTickets` y la publicacion del evento `OrdenPagada`, que `PublicadorDeOrdenPagada` envia a la cola despues del commit.
4. Si la confirmacion falla, revierte el cobro (`PasarelaDePago.revertir`) y relanza. El carrito queda intacto para reintentar.

El Facade y la transaccion estan en dos beans distintos a proposito. Spring aplica `@Transactional` con un proxy, y una llamada interna (`this.confirmar(...)`) no pasa por el proxy. Si el cobro y la transaccion compartieran clase, la anotacion no se aplicaria o envolveria el cobro. Por eso `VentaServiceImpl` no tiene `@Transactional` de clase.

El controller (`VentaController`) solo traduce HTTP a llamadas sobre `VentaService`: no conoce Eventos, Pagos ni Tickets.

## Consecuencias positivas

- Un cliente compra con una sola llamada: `POST /api/ventas/ordenes`.
- La regla "cobrar afuera, confirmar adentro, compensar si falla" vive en un solo lugar y tiene tests (`ConfirmarCompraTest`).
- Cambiar el proveedor de pago o sumar un paso (por ejemplo, el topico `ticket.emitido`) no cambia el contrato que usan los clientes.
- El contrato de Ventas solo expone DTO propios (`OrdenDTO`, `CarritoDTO`, `DatosDeTarjetaDTO`). Ninguna entidad sale del modulo.

## Consecuencias negativas

- Ventas es el modulo con mas dependencias salientes: Eventos, Usuarios, Pagos, Tickets y Notificaciones. Es el que mas cuidado necesita para no volverse un punto de acoplamiento excesivo (CLAUDE.md §4.6).
- La compra como invitado entra por un segundo punto, `CompraInvitadoService`, que reutiliza `PasarelaDePago` y `ConfirmacionDeCompra` pero no pasa por `VentaService`. El Facade no es la unica puerta a la compra. La logica transaccional si es compartida, asi que la regla no esta duplicada.
- Ventas es a la vez Facade y titular del carrito stateful (ADR-007). Separar un `ServicioDeInventario` sigue abierto (CLAUDE.md §10).
- La compensacion del cobro es best-effort: si revertir tambien falla, se devuelve 503 y el cobro necesita conciliacion manual.

## Evidencia en el repositorio

- `backend/src/main/java/com/passly/ventas/VentaService.java`: el contrato del Facade.
- `backend/src/main/java/com/passly/ventas/internal/negocio/VentaServiceImpl.java`: la orquestacion, sin `@Transactional` de clase.
- `backend/src/main/java/com/passly/ventas/internal/negocio/ConfirmacionDeCompra.java`: el limite transaccional y su Javadoc sobre el proxy.
- `backend/src/main/java/com/passly/ventas/package-info.java`: las dependencias declaradas del Facade.
- `backend/src/test/java/com/passly/ventas/ConfirmarCompraTest.java`: el camino feliz y el rollback con reversion del cobro.

## Criterio de revision

Revisar si Ventas suma dependencias hasta volverse dificil de explicar, si la compra como invitado diverge de la compra con cuenta, o si se separa el carrito en un componente de inventario. En ese caso, evaluar dividir el Facade en uno por tipo de compra o extraer el carrito.
