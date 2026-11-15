# ADR-008: Observer / mensajería para reaccionar a una orden pagada

- Estado: Aceptado (cola `orden.pagada` implementada en PAS-32; tópico `ticket.emitido` planificado)
- Fecha: 2026-10-26
- Responsables: Equipo Passly
- Alcance: `ventas` → `facturacion` (cola P2P); `tickets` → `notificaciones`, `accesos` (tópico)
- Tipo de patrón: GoF Observer (en proceso) + patrones de **integración** (Point-to-Point
  Channel y Publish-Subscribe Channel, EIP)

## Contexto

Cuando una compra se confirma, otras partes del sistema tienen que reaccionar: Facturación debe
generar la factura contra AFIP; más adelante Notificaciones debe mandar el mail con los QR y
Accesos precargar la validación. Ninguna de esas reacciones puede **frenar ni deshacer** la venta:
AFIP es lento y puede estar caído, y la transacción de compra no debe mantener filas bloqueadas
esperando a un externo.

## Fuerzas y restricciones

- La venta se confirma en una transacción local (`ConfirmacionDeCompra`); un rollback **no** debe
  anunciar una venta que no existió.
- Ventas no debe depender de Facturación ni de Notificaciones (grafo acíclico, verificado por
  Spring Modulith).
- Una orden no puede facturarse dos veces; un mail sí puede tener varios interesados.
- La consigna exige 1 cola P2P y 1 tópico pub/sub reales (Artemis, JMS).

## Alternativas consideradas

### Llamada síncrona de Ventas a `FacturacionService`

Simple, pero agrega la arista `ventas → facturacion`, mete la latencia de AFIP en la compra y una
caída de AFIP haría fallar ventas. Descartada.

### Observer en memoria solamente (`ApplicationEventPublisher`)

Desacopla en el código, pero si el proceso cae después del commit el evento se pierde y no hay
reintento ni DLQ. Sirve como primer tramo, no como integración.

### Tópico para todo

Con un tópico, dos instancias de Facturación facturarían la misma orden dos veces. Descartado
para facturación.

## Decisión

1. **Observer en proceso:** `ConfirmacionDeCompra` publica el evento de dominio `OrdenPagada`.
   `PublicadorDeOrdenPagada` lo observa con
   `@TransactionalEventListener(phase = AFTER_COMMIT)`: solo se entera de compras **confirmadas**.
2. **Point-to-Point Channel:** el publicador envía un `TextMessage` JSON (sin nombres de clase
   Java en headers) a la cola durable `orden.pagada` de Artemis. Un solo consumidor
   (`OrdenPagadaListener`) registra la factura `PENDIENTE_AFIP`. Idempotencia por restricción
   única + `INSERT ... ON CONFLICT`; redelivery y DLQ configurados en `infra/artemis/broker.xml`.
3. **Publish-Subscribe Channel (Obligatoria 2):** `ticket.emitido` como tópico, con
   Notificaciones y Accesos como suscriptores independientes.

## Consecuencias positivas

- Ventas no conoce a Facturación: `allowedDependencies = {}` en `facturacion` y sin arista Java.
- Si AFIP o Facturación están caídos, la venta se confirma igual y el mensaje espera en la cola.
- Sumar un suscriptor nuevo al tópico no toca a quien publica.

## Consecuencias negativas

- **Sin outbox:** si el broker cae justo después del commit, el mensaje se pierde y queda un log
  `ERROR` con el id de la orden (deuda declarada; candidato: Event Publication Registry de Spring
  Modulith).
- Consistencia eventual: durante segundos puede haber orden sin factura.
- Más infraestructura (Artemis en Docker) y debugging distribuido.

## Evidencia en el repositorio

- `ventas/internal/negocio/OrdenPagada.java`, `PublicadorDeOrdenPagada.java`,
  `PublicadorDeOrdenPagadaTest.java`.
- `facturacion/internal/web/OrdenPagadaListener.java`, `MensajeriaIntegrationTest.java`.
- `docs/mensajeria.md`, `docs/integraciones.md` §4 y §7.

## Criterio de revisión

Revisar antes de la Obligatoria 2: agregar outbox / Event Publication Registry si la pérdida de
mensajes post-commit deja de ser aceptable.
