# Passly · Segunda entrega parcial

**UADE · Desarrollo de Software II · 16 de noviembre de 2026**\
**Equipo:** Federico Torcini, Juan Segundo Addamo, Lucio Dillon y Simón Forteza.

## Evolución del producto

En la Obligatoria 1 se estableció el monolito modular, con módulos organizados en presentación, negocio y datos. El carrito ya mantenía la selección durante cinco minutos y el informe documentó el límite transaccional de la compra.

En el Checkpoint 2 se integró la compra con tarjeta por REST contra una pasarela mock, la emisión de QR firmados y la validación de uso único. El proceso de venta publica en ActiveMQ Artemis al confirmar la orden. Facturación consume `orden.pagada` y registra una factura por orden en `PENDIENTE_AFIP`. La integración fiscal SOAP corresponde a una etapa posterior.

Para la segunda entrega, el backend emite JWT de dos horas y la web lo envía como Bearer. Las operaciones sensibles conservan la autorización por rol y por pertenencia a una productora. El carrito mantiene su propia sesión HTTP. Se sumaron pruebas de autenticación y la colección de peticiones se actualizó al login con JWT. La siguiente tabla ubica cada requisito y señala qué quedaba por verificar al preparar la exposición.

## Checklist de la consigna del 16/11

| Requisito | Evidencia a mostrar | Estado al preparar esta versión |
|---|---|---|
| Componentes desplegados y en capas | Compose, perfil demo, web, `/actuator/modulith` y prueba de fronteras | Componentes y estructura en `main`. |
| Flujo punta a punta | Enlace público, tarjeta de prueba, QR, «VÁLIDO» y «YA USADO» | Compra y dos validaciones comprobadas en la web y por API contra una base nueva. |
| JWT y acceso por rol | Login, 403 para comprador que intenta crear un evento y 401 para token inválido | PAS-33 y PAS-34 integrados. Respuestas 403 y 401 comprobadas. |
| State y 409 | Republicar, cancelar dos veces y mostrar la clase de estado | Republicación disponible. Cancelación y clases de estado pendientes de PAS-35 al corte. |
| Mensajería en un proceso de negocio | Artemis, cola `orden.pagada` y consulta a Facturación | PAS-32 integrado. Pruebas de Artemis y Facturación aprobadas. |
| Pruebas unitarias y CI | `./mvnw test` y ejecución de GitHub Actions del commit presentado | 88 pruebas aprobadas en local. CI verifica fronteras. Ampliación y cobertura en PAS-38. |
| Un ADR por patrón | ADR y clase correspondiente, por ejemplo `VentaService` | ADR de arquitectura disponibles. ADR de patrones en PAS-36. |
| Tablero y feedback de diseño | Tablero del equipo, matriz de historias y observaciones con su resolución | Estados y responsables relevados. Confirmación del tablero y devoluciones de la cátedra pendientes. |
| Documentación y presentación | Este informe y el guion de demo | Preparados para revisión del equipo. Ensayo previsto para el 15/11. |

**Referencias:** `docs/entrega-2/guion-demo.md`, `docs/trazabilidad.md`, `docs/feedback-diseno.md` y `docs/plan-entregas.md`.

## Declaración de uso de IA

Para esta entrega se usó **Codex (OpenAI)** para contrastar historias, issues y cambios de Git, redactar el guion y el informe, generar el PDF y asistir con la verificación técnica. El equipo debe validar los resultados y completar esta declaración con otras herramientas utilizadas por sus integrantes antes de presentar.
