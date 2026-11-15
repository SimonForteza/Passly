# Registro de decisiones de arquitectura (ADR)

Cada ADR explica una decision: el problema, las alternativas que se evaluaron, por que se eligio una y que cuesta. Los de patrones apuntan a las clases reales donde esta aplicado cada uno, para poder contrastarlos con el codigo.

| ADR | Decision | Tipo | Estado | Fecha | Donde mirar en el codigo |
|---|---|---|---|---|---|
| [ADR-001](ADR-001-monolito-modular-vs-microservicios.md) | Monolito modular en lugar de microservicios | Arquitectura | Aceptado | 2026-09-11 | `EstructuraDeModulosTest`, `package-info.java` de cada modulo |
| [ADR-002](ADR-002-esquema-por-componente-vs-base-por-componente.md) | Esquema PostgreSQL por componente | Arquitectura | Aceptado | 2026-09-11 | `@Table(schema = ...)` de cada entidad, `db/init/` |
| [ADR-003](ADR-003-facade-servicio-de-ventas.md) | Facade para la compra | Patron | Aceptado | 2026-11-15 | `VentaService`, `VentaServiceImpl`, `ConfirmacionDeCompra` |
| [ADR-004](ADR-004-adapter-y-port-proveedores-externos.md) | Port y Adapter para los proveedores externos | Patron | Aceptado | 2026-11-15 | `PasarelaDePago`, `PasarelaDePagoReal`, `PagoServiceImpl`, `ClienteAfip` |
| [ADR-005](ADR-005-dao-repository-spring-data.md) | DAO con repositorios de Spring Data JPA | Patron | Aceptado | 2026-11-15 | `*/internal/datos/*Repository.java` |
| [ADR-006](ADR-006-state-ciclo-de-vida-del-evento.md) | State para el ciclo de vida del evento | Patron | Aceptado | 2026-11-15 | `EstadoDeEvento`, `Borrador`, `Publicado`, `Cancelado`, `Evento` |
| [ADR-007](ADR-007-componente-stateful-carrito-session-scope.md) | Carrito stateful con `@SessionScope` | Componente stateful | Aceptado | 2026-11-15 | `CarritoDeCompra`, `CarritoInvitado` |
| [ADR-008](ADR-008-observer-pubsub-orden-pagada.md) | Observer y canales de mensajeria para reaccionar a una orden pagada | Patron (integracion) | Aceptado | 2026-10-26 | `OrdenPagada`, `PublicadorDeOrdenPagada`, `OrdenPagadaListener`, cola `orden.pagada` |
| [ADR-009](ADR-009-grasp-pure-fabrication-confirmacion-de-compra.md) | Pure Fabrication para el limite transaccional de la compra | Patron (GRASP) | Aceptado | 2026-10-26 | `ConfirmacionDeCompra`, `VentaServiceImpl` |

## Como agregar un ADR

1. Copiar el formato de un ADR existente: Estado, Fecha, Responsables, Alcance, Contexto, Fuerzas y restricciones, Alternativas consideradas, Decision, Consecuencias positivas y negativas, Evidencia en el repositorio y Criterio de revision.
2. Numerar con el siguiente numero libre (`ADR-010-...`). No renumerar los existentes.
3. Si una decision cambia, no se borra el ADR viejo: se marca como `Reemplazado por ADR-NNN` y se escribe uno nuevo.
4. Agregar la fila a esta tabla y linkear el ADR desde CLAUDE.md §4.9 si es un patron.
