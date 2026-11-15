# Passly — Esqueleto documental (Clase 12, Parte B)

> Esqueleto de la documentación de negocio, funcional y de usuario, trazado contra los patrones de
> [patrones.md](patrones.md). Lo marcado ✅ ya existe en el repo; ⏳ es contenido a escribir.

## 4. BRD — Business Requirements Document

### 4.1 Índice completo

1. **Resumen ejecutivo** — problema (venta y control de acceso de entradas para productoras
   independientes), propuesta de valor, alcance de la versión 1.
2. **Contexto y oportunidad** — situación actual (planillas, listas en puerta, reventa de
   capturas de QR), actores del mercado, por qué ahora.
3. **Objetivos de negocio** — medibles, con plazo (ver §4.2).
4. **Alcance**
   4.1 Dentro del alcance (los cinco recorridos de la visión, CLAUDE.md §1)
   4.2 Fuera del alcance (reventa, asientos numerados, app nativa del comprador)
5. **Interesados (stakeholders)** — productora (dueño, staff, validador), comprador con cuenta,
   comprador invitado, administrador de la plataforma, AFIP, proveedor de pagos.
6. **Requisitos de negocio** — BR-xx con métrica y prioridad MoSCoW (§4.2).
7. **Reglas de negocio** — carrito de 5 min sin reserva de cupo, QR de uso único, evento publicado
   solo se amplía, sin datos de tarjeta guardados (ver `funcional.md` §4).
8. **Supuestos y dependencias** — pasarela de pago disponible, AFIP homologación, broker JMS.
9. **Restricciones** — académicas (stack Spring Boot, entregas), legales (facturación electrónica,
   datos personales — Ley 25.326), PCI (no almacenar tarjeta).
10. **Riesgos** — caída de AFIP, caída de la pasarela, fraude con QR duplicados, picos de venta.
11. **Indicadores de éxito (KPI)** y cómo se miden.
12. **Glosario** — evento, tipo de entrada, cupo, orden, ticket, productora, validación.
13. **Trazabilidad** — matriz BR → HU → criterio → patrón/componente (§7).
14. **Aprobaciones y control de versiones.**

### 4.2 Requisitos de negocio (3, con métrica y MoSCoW)

| ID | Requisito de negocio | Métrica de aceptación | MoSCoW | Justificación de la prioridad |
|---|---|---|---|---|
| **BR-01** | Una vez que un evento está a la venta, sus condiciones (nombre, fecha, lugar, precio de lo ya ofrecido) **no pueden alterarse** en perjuicio de quien compró; solo se puede ampliar la oferta. | 100 % de los intentos de modificar o republicar un evento publicado se rechazan (409) y quedan sin efecto; 0 reclamos por cambio de condiciones post-compra. | **Must** | Sin esto se pierde la confianza del comprador y aparece riesgo legal (Ley 24.240, oferta vinculante). |
| **BR-02** | El comprador paga con tarjeta online a través de un **proveedor de pagos externo reemplazable**, sin que una falla del proveedor deje cobros sin orden. | ≥ 95 % de compras confirmadas en < 10 s (p95); 0 cobros aprobados sin orden ni reversión; cambiar de proveedor sin modificar el flujo de venta. | **Must** | Es la fuente de ingresos de la plataforma y de la productora. |
| **BR-03** | Toda orden pagada se **factura electrónicamente** ante AFIP sin demorar ni bloquear la venta, aun si AFIP no responde. | 100 % de órdenes pagadas con factura registrada (pendiente o emitida) en < 5 min; 0 ventas rechazadas por indisponibilidad de AFIP; 0 facturas duplicadas por orden. | **Should** | Obligación fiscal, pero la venta puede regularizarse después: no debe bloquear la operación. |

Para completar el MoSCoW del BRD: **Could** — recordatorio por mail 24 h antes del evento;
**Won't (esta versión)** — reventa entre compradores con transferencia de QR.

## 5. Funcional — 3 historias de usuario con criterios Gherkin

### HU-08 · Publicar y editar un evento — traza a **BR-01** · patrón **State** (ADR-006)

**Como** organizador de una productora **quiero** publicar un evento en borrador y, una vez
publicado, solo poder ampliarlo **para** no alterar lo que ya le vendí a nadie.

```gherkin
Característica: Ciclo de vida de un evento

  Antecedentes:
    Dado que soy organizador de la productora "Aurora"
    Y que "Aurora" tiene el evento "Fiesta de Primavera" con fecha futura

  Escenario: HU-08.C1 Publicar un borrador con cupo
    Dado que el evento está en estado "BORRADOR"
    Y que tiene el tipo de entrada "General" con 100 lugares
    Cuando solicito publicarlo
    Entonces la respuesta es 200
    Y el evento queda en estado "PUBLICADO"
    Y aparece en la cartelera

  Escenario: HU-08.C2 Republicar un evento publicado es un conflicto
    Dado que el evento está en estado "PUBLICADO"
    Cuando solicito publicarlo otra vez
    Entonces la respuesta es 409 Conflict
    Y el detalle del error indica que el evento ya está "PUBLICADO"
    Y el evento sigue en estado "PUBLICADO"

  Escenario: HU-08.C3 Un evento publicado no admite cambiar sus datos
    Dado que el evento está en estado "PUBLICADO"
    Cuando intento cambiar la fecha del evento
    Entonces la respuesta es 409 Conflict
    Y la fecha del evento no cambia

  Escenario: HU-08.C4 Un evento publicado sí admite ampliar cupo
    Dado que el evento está en estado "PUBLICADO"
    Cuando amplío en 50 el cupo de "General"
    Entonces la respuesta es 200
    Y "General" tiene 150 lugares en total
```

### HU-03 · Comprar con tarjeta — traza a **BR-02** · patrones **Adapter** (ADR-004) y **Pure Fabrication** (ADR-009)

**Como** comprador **quiero** pagar mis entradas con tarjeta en el checkout **para** recibir mis
QR en el momento, sin que un problema del pago me deje cobrado sin entradas.

```gherkin
Característica: Compra con tarjeta

  Antecedentes:
    Dado que inicié sesión como comprador
    Y que tengo en el carrito 2 entradas "General" de un evento publicado

  Escenario: HU-03.C1 Pago aprobado
    Cuando confirmo la compra con una tarjeta de prueba aprobada
    Entonces se crea una orden pagada
    Y recibo 2 tickets, cada uno con su QR firmado
    Y el cupo de "General" baja en 2

  Escenario: HU-03.C2 Pago rechazado por el proveedor
    Cuando confirmo la compra con una tarjeta de prueba rechazada
    Entonces la respuesta es 402
    Y no se crea ninguna orden
    Y mi carrito sigue intacto para reintentar

  Escenario: HU-03.C3 Falla después de cobrar: se revierte el cobro
    Dado que una de las líneas del carrito ya no tiene cupo suficiente
    Cuando confirmo la compra con una tarjeta de prueba aprobada
    Entonces el cobro se revierte
    Y no se crea ninguna orden
    Y el cupo de todas las líneas queda como estaba
```

### HU-15 · Facturar cada orden pagada sin frenar la venta — traza a **BR-03** · patrón **Observer + Point-to-Point Channel** (ADR-008)

**Como** dueño de una productora **quiero** que cada compra confirmada genere su factura
automáticamente **para** cumplir con AFIP sin que una caída de AFIP me haga perder ventas.

```gherkin
Característica: Facturación asíncrona de órdenes pagadas

  Escenario: HU-15.C1 Una orden pagada genera una factura pendiente
    Dado que un comprador confirmó una compra
    Cuando el mensaje "orden.pagada" llega a Facturación
    Entonces existe una factura en estado "PENDIENTE_AFIP" para esa orden

  Escenario: HU-15.C2 Un mensaje duplicado no duplica la factura
    Dado que ya existe una factura para la orden 42
    Cuando llega otra vez el mensaje "orden.pagada" de la orden 42
    Entonces sigue habiendo una sola factura para la orden 42

  Escenario: HU-15.C3 Facturación caída no bloquea la venta
    Dado que el consumidor de "orden.pagada" está detenido
    Cuando un comprador confirma una compra
    Entonces la compra se confirma igual
    Y el mensaje queda en la cola hasta que el consumidor vuelve

  Escenario: HU-15.C4 Una compra revertida no se anuncia
    Dado que la confirmación de una compra falla y se hace rollback
    Entonces no se publica ningún mensaje "orden.pagada"
```

## 6. Manual de usuario — índice por rol, clasificado según Diátaxis

**Diátaxis:** **T** = Tutorial (aprender haciendo, recorrido guiado) · **G** = Guía práctica
(cómo lograr una tarea concreta) · **R** = Referencia (descripción exacta, para consultar) ·
**E** = Explicación (por qué funciona así).

| Rol | Sección | Tipo | Estado |
|---|---|---|---|
| **Comprador** | 1.1 Tu primera compra en Passly, paso a paso (con la tarjeta de prueba) | T | ✅ (`manual-de-usuario.md` §1.1–1.2) |
| | 1.2 Comprar sin crear una cuenta | G | ✅ §1.3 |
| | 1.3 Crear una cuenta y volver al evento | G | ✅ §1.4 |
| | 1.4 Ver mis entradas y descargar los QR | G | ✅ §1.5 |
| | 1.5 Recuperar el enlace de una compra como invitado | G | ✅ (`/orden-invitado`) |
| | 1.6 Estados de una orden y de un ticket | R | ⏳ |
| | 1.7 Por qué el carrito vence a los 5 minutos y no reserva lugar | E | ⏳ |
| **Organizador** (dueño / staff) | 2.1 Tu primer evento: crear la productora, cargar y publicar | T | ✅ §2.1–2.3 |
| | 2.2 Compartir el link de un evento | G | ✅ §2.4 |
| | 2.3 Invitar staff y validadores | G | ✅ §2.5 |
| | 2.4 Ampliar el cupo o sumar un tipo de entrada a un evento publicado | G | ⏳ |
| | 2.5 Estados del evento y qué se puede editar en cada uno (diagrama de estados) | R | ⏳ — sale de `patrones.md` §2 |
| | 2.6 Por qué un evento publicado no se puede modificar | E | ⏳ — sale de BR-01 / ADR-006 |
| | 2.7 Facturación de las ventas: qué significa "pendiente AFIP" | E | ⏳ — sale de ADR-008 |
| **Validador** | 3.1 Tu primer turno en la puerta: escanear un QR | T | ✅ §3 |
| | 3.2 Validar con el código manual si la cámara falla | G | ✅ §3 |
| | 3.3 Resultados de la validación (válida, ya usada, otro evento, inválida) | R | ✅ §3 |
| | 3.4 Por qué un QR no se puede usar dos veces aunque lo escaneen a la vez | E | ⏳ |
| **Administrador** | 4.1 Dar de alta organizadores y validadores | G | ✅ §4 |
| | 4.2 Roles globales y roles dentro de una productora | R | ⏳ |
| **Todos** | 5. Problemas frecuentes ("Tu carrito venció", "Pago rechazado", "Ya usado"…) | G | ✅ §5 |
| | 6. Glosario | R | ⏳ |

Lectura del índice: hoy el manual está cargado de tutoriales y guías; faltan **referencias** y
**explicaciones**. Las que faltan salen directamente de los ADR y del diagrama de estados, así que
la documentación técnica alimenta la de usuario.

## 7. Matriz de trazabilidad mínima: BR → HU → criterio → patrón / componente

| BR | HU | Criterio | Patrón | Componente / clase | Evidencia (test) |
|---|---|---|---|---|---|
| BR-01 | HU-08 | HU-08.C1 publicar borrador | State | `eventos` · `Borrador.publicar` | `MatrizDeEstadosDeEventoTest.publicarUnBorradorLoLlevaAPublicado`, `publicarUnBorradorConFechaPasadaLanzaTransicionInvalida`, `publicarUnBorradorSinCupoLanzaTransicionInvalida` |
| BR-01 | HU-08 | HU-08.C2 republicar → 409 | State | `Publicado.publicar` · `ManejadorDeErrores` | `MatrizDeEstadosDeEventoTest.cadaEstadoDecideQueOperacionesAdmite`, `CancelacionDeEventoTest.publicarUnEventoCanceladoDa409` |
| BR-01 | HU-08 | HU-08.C3 publicado no edita datos | State | `Publicado.editarDatos` / `Publicado.editarTipoEntrada` | `MatrizDeEstadosDeEventoTest.cadaEstadoDecideQueOperacionesAdmite`, `EdicionDeEventoTest.editarDatosDeUnEventoPublicadoLanzaEdicionInvalida` |
| BR-01 | HU-08 | HU-08.C4 publicado amplía cupo | State | `Publicado.ampliarCupo` | `EdicionDeEventoTest.ampliarCupoEnUnEventoPublicadoConVentasPreviasSumaAlTotalYAlDisponible` |
| BR-02 | HU-03 | HU-03.C1 pago aprobado | Adapter + Pure Fabrication + Facade | `PasarelaDePagoReal` → `PagoService`; `ConfirmacionDeCompra`; `VentaService` | `ConfirmarCompraTest.confirmarCompraEmiteUnTicketPorCadaUnidad` |
| BR-02 | HU-03 | HU-03.C2 pago rechazado → 402 | Adapter | `PasarelaDePagoReal` (traduce `PagoRechazadoException`) | `ConfirmarCompraTest.confirmarCompraConTarjetaRechazadaDevuelve402` |
| BR-02 | HU-03 | HU-03.C3 revertir tras falla | Pure Fabrication + Adapter | `ConfirmacionDeCompra` (rollback) · `PasarelaDePago.revertir` | `ConfirmarCompraTest.elRollbackDevuelveElCupoYNoDejaOrden` ⚠️ puede dar rojo en una base local creada antes de `REVERTIDO`: `ddl-auto: update` no actualiza el `CHECK` de `pagos.pago.estado` (deuda Flyway, CLAUDE.md §10) |
| BR-03 | HU-15 | HU-15.C1 factura pendiente | Observer + Point-to-Point Channel | `PublicadorDeOrdenPagada` → cola `orden.pagada` → `OrdenPagadaListener` | `MensajeriaIntegrationTest.compraConfirmadaEsperaEnElBrokerHastaReactivarConsumidor` |
| BR-03 | HU-15 | HU-15.C2 sin duplicados | Idempotent Receiver | `FacturacionServiceImpl` (`INSERT … ON CONFLICT`) | `MensajeriaIntegrationTest.mensajeDuplicadoGeneraUnaSolaFactura` |
| BR-03 | HU-15 | HU-15.C3 consumidor caído | Point-to-Point Channel (cola durable) | Artemis `orden.pagada`, DLQ | `MensajeriaIntegrationTest.compraConfirmadaEsperaEnElBrokerHastaReactivarConsumidor`, `mensajeInvalidoReintentaYTerminaEnDlq` |
| BR-03 | HU-15 | HU-15.C4 rollback no anuncia | Observer (`AFTER_COMMIT`) | `PublicadorDeOrdenPagada` | ⚠️ sin test automatizado: lo garantiza `@TransactionalEventListener(AFTER_COMMIT)`; `PublicadorDeOrdenPagadaTest` solo cubre la caída del broker. Tarea pendiente. |
