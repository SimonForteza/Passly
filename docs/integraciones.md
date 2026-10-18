# Estado de las integraciones

| Origen | Destino | Medio | Estado |
|---|---|---|---|
| Ventas | Facturacion | JMS P2P, cola durable `orden.pagada` | Implementada en PAS-32: AFTER_COMMIT, JSON, consumo transaccional e insercion idempotente |
| Tickets | Notificaciones y Accesos | JMS pub/sub, topico `ticket.emitido` | Pendiente, Obligatoria 2 |
| Facturacion | AFIP | SOAP | Pendiente; solo puerto ClienteAfip y adapter de log. Facturas PENDIENTE_AFIP |

El contrato de orden.pagada contiene idOrden, idComprador, total y cobradaEn. Facturacion no importa clases de Ventas: la dependencia de negocio es asincrona y no una arista Java del grafo de Modulith.

El guion de acumulacion de mensajes, reactivacion, duplicados y DLQ esta en [mensajeria.md](mensajeria.md).
La garantia es entrega con posibles duplicados y efecto idempotente en Facturacion. El envio posterior al commit todavia no tiene outbox; si falla, queda conciliacion manual.

Este documento registra el estado comprobable de PAS-32. El diagrama SOA general corresponde a PAS-27.
