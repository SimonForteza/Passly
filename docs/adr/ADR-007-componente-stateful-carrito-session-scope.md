# ADR-007: Carrito stateful con @SessionScope en ServicioDeVentas

- Estado: Aceptado
- Fecha: 2026-11-15
- Responsables: Equipo Passly
- Alcance: Componente `ventas`

## Contexto

La consigna pide al menos un componente stateful y uno stateless, justificados. Durante una compra, el comprador arma un carrito en varias llamadas (agrega entradas, lo revisa, paga) y esa seleccion vale por un tiempo limitado: 5 minutos desde que se crea.

Ese estado es **conversacional**: dura lo que dura la compra y no es un dato del negocio. Persistir datos no vuelve stateful a un componente. Facturacion guarda facturas y sigue siendo stateless (CLAUDE.md §4.7).

## Fuerzas y restricciones

- La consigna evalua la gestion del ciclo de vida por el contenedor (Unidad II).
- La autenticacion es stateless: JWT con `SessionCreationPolicy.STATELESS` (PAS-33).
- Nadie tiene que poder ver o usar el carrito de otro.
- El carrito tiene que vencer a los 5 minutos aunque la sesion siga viva.
- El cupo vive solo en `eventos.tipo_entrada`. El carrito no puede convertirse en una segunda fuente de stock.

## Alternativas consideradas

### Persistir el carrito en base o en cache (Redis)

Vuelve stateless a Ventas y escala horizontalmente sin sesiones pegadas. Es lo que se haria en produccion. Descartada para este TP: no muestra la gestion del ciclo de vida por el contenedor, que es justamente lo que se evalua, y suma una tabla o un servicio mas.

### Carrito en el cliente

La app web guarda la seleccion y la manda completa al confirmar. Descartada: el backend no tendria estado conversacional que mostrar, y el precio y el vencimiento dependerian del cliente.

### Bean `@SessionScope` administrado por Spring

Una instancia del carrito por sesion HTTP, que el contenedor crea y destruye.

## Decision

`ventas.internal.negocio.CarritoDeCompra` es un `@Component @SessionScope`:

- **Ciclo de vida del contenedor:** `@PostConstruct` (`alCrear`) y `@PreDestroy` (`alDestruir`) dejan un log. Es la evidencia en vivo de que Spring decide cuando nace y cuando muere. `DELETE /api/ventas/carrito` invalida la sesion para mostrar el `@PreDestroy` en el momento.
- **Dos relojes distintos:** `expiraEn` es la regla de negocio (5 minutos absolutos desde la creacion) y se valida en `itemsParaConfirmar` (`CarritoVencidoException`). `server.servlet.session.timeout: 5m` es la limpieza del contenedor. El `@PreDestroy` puede aparecer bastante despues de `expiraEn`, porque el limpiador de sesiones de Tomcat no corre al segundo.
- **Chequeo de dueno:** `STATELESS` solo afecta a Spring Security. El contenedor igual crea la `HttpSession` que pide el proxy del bean. Nada asocia ese `JSESSIONID` al usuario autenticado, asi que el carrito guarda el id del comprador en su primera operacion y lo compara en cada llamada (`verificarDueño`, `CarritoDeOtroCompradorException` -> 409).
- **No reserva cupo:** el carrito guarda precio y nombre resueltos por `EventoService.consultarDisponibilidad`. El cupo se descuenta recien al confirmar (ADR-003). Puede haber sobreventa optimista entre carritos simultaneos: el segundo en confirmar recibe 409.
- **Compra como invitado:** `CarritoInvitado` (tambien `@SessionScope`) envuelve un `CarritoDeCompra` y suma un token propietario y un candado para requests concurrentes de la misma sesion.

El resto de los componentes son singletons stateless.

## Consecuencias positivas

- Muestra el ciclo de vida administrado por el contenedor sin codigo de infraestructura propio.
- El carrito no necesita tabla ni limpieza manual: muere con la sesion.
- El chequeo de dueno convierte un riesgo real de la sesion en una regla explicita y probada.
- El cupo sigue teniendo un solo dueno (Eventos), sin duplicarse.

## Consecuencias negativas

- No escala horizontalmente sin sesiones pegadas o replicacion de sesion. Es el trade-off declarado.
- Spring Security cambia el `JSESSIONID` en cada request autenticado (proteccion contra *session fixation*, sigue pasando con JWT). El frontend tiene que encolar los requests autenticados, y recargar la pagina con un request en vuelo puede perder el carrito. Desactivar la rotacion queda como decision pendiente (CLAUDE.md §10).
- Despues de comprar, el reloj no se reinicia. El frontend invalida la sesion para empezar un carrito nuevo.
- Sin reserva de cupo, un comprador puede llegar a pagar la pantalla y recibir 409 porque otro confirmo antes.
- Ventas es a la vez Facade (ADR-003) y titular del carrito.

## Evidencia en el repositorio

- `backend/src/main/java/com/passly/ventas/internal/negocio/CarritoDeCompra.java` y `CarritoInvitado.java`.
- `backend/src/main/resources/application.yml`: `server.servlet.session.timeout: 5m`.
- `backend/src/main/java/com/passly/ventas/CarritoDeOtroCompradorException.java` y `CarritoVencidoException.java`.
- `backend/src/test/java/com/passly/ventas/internal/negocio/CarritoDeCompraTest.java`.
- `docs/http/08-ventas.http` y `docs/http/README.md`: el carrito en vivo y la rotacion del `JSESSIONID`.

## Criterio de revision

Revisar si hace falta escalar Ventas en mas de una instancia (el punto extra de escalabilidad del 07/12) o si se decide reservar cupo. En ambos casos, el carrito pasa a la base o a una cache y Ventas queda stateless. Este ADR se reemplaza por uno nuevo.
