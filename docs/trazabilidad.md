# Trazabilidad de historias de usuario

Corte del 14/11/2026 sobre `main` (`e95a4ac`). La [Obligatoria 1](entrega-o1/informe-obligatoria-1.md) describe el recorrido de publicación, venta y validación. El [inventario funcional del Checkpoint 2](funcional.md) enumera los identificadores HU-01 a HU-14. La matriz toma esos identificadores como referencia y mantiene visibles las historias que ese inventario registra como planificadas. Queda pendiente cotejar el alcance con el material de diseño remitido a la cátedra si existe una versión distinta.

| Historia | Issues de alcance o implementación | Estado al corte | Evidencia para pruebas |
|---|---|---|---|
| HU-01 · Cartelera y página de productora | [PAS-12](https://linear.app/passly/issue/PAS-12), [PAS-17](https://linear.app/passly/issue/PAS-17), [PAS-26](https://linear.app/passly/issue/PAS-26) | Implementada | `docs/http/02-cartelera.http` |
| HU-02 · Llegar por el link de un evento | [PAS-25](https://linear.app/passly/issue/PAS-25) | Implementada | `docs/http/11-link-compartible.http` |
| HU-03 · Pagar con tarjeta y recibir QR | [PAS-8](https://linear.app/passly/issue/PAS-8), [PAS-20](https://linear.app/passly/issue/PAS-20), [PAS-29](https://linear.app/passly/issue/PAS-29), [PAS-30](https://linear.app/passly/issue/PAS-30), [PAS-37](https://linear.app/passly/issue/PAS-37) | Compra implementada. Sigue abierto el control de estados vendibles (PAS-37). | `docs/http/08-ventas.http` |
| HU-04 · Comprar como invitado y recuperar la orden | [PAS-42](https://linear.app/passly/issue/PAS-42), [PAS-30](https://linear.app/passly/issue/PAS-30) | Implementada. El flujo de recuperación se integró en [PR #27](https://github.com/SimonForteza/Passly/pull/27). | `docs/http/12-ventas-invitados.http` |
| HU-05 · Registrarse o iniciar sesión y volver al evento | [PAS-5](https://linear.app/passly/issue/PAS-5), [PAS-15](https://linear.app/passly/issue/PAS-15), [PAS-25](https://linear.app/passly/issue/PAS-25), [PAS-34](https://linear.app/passly/issue/PAS-34) | Implementada | Manual, secciones de registro y vuelta al evento |
| HU-06 · Ver mis entradas y descargar los QR | [PAS-18](https://linear.app/passly/issue/PAS-18), [PAS-20](https://linear.app/passly/issue/PAS-20) | Implementada | Manual, sección Mis entradas |
| HU-07 · Crear productora | [PAS-13](https://linear.app/passly/issue/PAS-13), [PAS-16](https://linear.app/passly/issue/PAS-16) | Implementada. El alta directa desde la web se integró en [PR #27](https://github.com/SimonForteza/Passly/pull/27). | `docs/http/05-productoras.http` |
| HU-08 · Crear, editar y publicar evento | [PAS-12](https://linear.app/passly/issue/PAS-12), [PAS-17](https://linear.app/passly/issue/PAS-17), [PAS-19](https://linear.app/passly/issue/PAS-19), [PAS-35](https://linear.app/passly/issue/PAS-35) | Implementada. PAS-35 cambia cómo se aplican las reglas de transición. | `docs/http/10-edicion-eventos.http` |
| HU-09 · Compartir el link del evento | [PAS-25](https://linear.app/passly/issue/PAS-25) | Implementada | Manual, gestión del evento |
| HU-10 · Invitar a staff y validadores | [PAS-43](https://linear.app/passly/issue/PAS-43), [PAS-13](https://linear.app/passly/issue/PAS-13), [PAS-16](https://linear.app/passly/issue/PAS-16) | Implementada. El flujo de invitación por correo se integró en [PR #27](https://github.com/SimonForteza/Passly/pull/27). | `docs/invitaciones-y-compra-invitada.md` |
| HU-11 · Validar el QR con uso único | [PAS-20](https://linear.app/passly/issue/PAS-20), [PAS-31](https://linear.app/passly/issue/PAS-31) | Implementada en la web. La app móvil nativa sigue planificada. | `docs/http/09-accesos.http` y `/validar` |
| HU-12 · Dar de alta usuarios con roles especiales | [PAS-5](https://linear.app/passly/issue/PAS-5), [PAS-6](https://linear.app/passly/issue/PAS-6), [PAS-15](https://linear.app/passly/issue/PAS-15), [PAS-33](https://linear.app/passly/issue/PAS-33) | Implementada | `docs/http/07-seguridad.http` |
| HU-13 · Recibir entradas por correo con cuenta | [PAS-41](https://linear.app/passly/issue/PAS-41) | Planificada. PAS-41 retoma la parte de PAS-22 que corresponde a esta historia. | `docs/integraciones.md` y aceptación de PAS-41 |
| HU-14 · Cancelar un evento y avisar a los compradores | [PAS-35](https://linear.app/passly/issue/PAS-35), [PAS-37](https://linear.app/passly/issue/PAS-37) | Planificada al corte. PAS-35 cubre cancelación y PAS-37 protege la venta. Avisos y reintegros figuran como pendientes del plan. | Aceptación de PAS-35 y PAS-37 |

## Tablero del 16/11

Responsables y estados en Linear al 14/11/2026, antes de enviar los cambios de PAS-39 y PAS-40 a revisión.

| Issue | Responsable | Estado |
|---|---|---|
| [PAS-33](https://linear.app/passly/issue/PAS-33) | Juan Segundo Addamo | Done |
| [PAS-34](https://linear.app/passly/issue/PAS-34) | Juan Segundo Addamo | Done |
| [PAS-35](https://linear.app/passly/issue/PAS-35) | Lucio Dillon | In Progress |
| [PAS-36](https://linear.app/passly/issue/PAS-36) | Lucio Dillon | Todo |
| [PAS-37](https://linear.app/passly/issue/PAS-37) | Simón Forteza | Todo |
| [PAS-38](https://linear.app/passly/issue/PAS-38) | Simón Forteza | In Progress |
| [PAS-39](https://linear.app/passly/issue/PAS-39) | FedeTor | Todo |
| [PAS-40](https://linear.app/passly/issue/PAS-40) | FedeTor | Todo |

El [equipo Passly en Linear](https://linear.app/passly) mantiene los estados actuales. Para el 30/11, esta matriz sirve como base para vincular cada criterio aplicable con un caso de prueba, el test que lo ejecuta y su resultado.
