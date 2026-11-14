# Plan de entregas — lo que falta para aprobar Passly

> **Fuente única de "qué falta".** Armado el **14/11/2026** a partir del mensaje del profe
> ("TPO: entregas que quedan y horarios de exposición") y contrastado contra `main`
> (`b677f7e`, 19/10). Cada issue que se cierra tacha su fila acá **y** actualiza CLAUDE.md §9.
> Si una consigna nueva contradice CLAUDE.md, manda este archivo.

**Estado:** ✅ hecho en `main` · 🟡 parcial · ❌ falta · ❓ sin verificar

## Fechas

| Entrega | Fecha | Carácter |
|---|---|---|
| 2.ª Entrega Parcial | **16/11** | El núcleo funcionando, en vivo |
| 3er Checkpoint | **30/11** | Integración completa + ensayo de testing (G1 a G9) |
| Entrega final | **07/12** | Prueba del sistema completo. G1 a G9 **eliminatorias** |
| Recuperatorio | a confirmar | Mostrar qué se corrigió y dónde |

Horarios: el 16/11 y el 07/12 van de G1 (13:30) a G13 (16:30), cada 15 minutos. El 30/11 el
orden se invierte. Se pueden intercambiar turnos entre grupos avisando por Teams antes. **Llegar
antes**: si el equipo no está en su horario, queda *Ausente*.

Reglas generales: cada entrega se construye sobre la anterior, **todo se muestra funcionando en
vivo** y el alcance que se evalúa son **las historias de usuario comprometidas en la entrega del
diseño**.

---

## 1. 2.ª Entrega Parcial — 16/11

| Requisito de la consigna | Estado | Issue | Responsable |
|---|---|---|---|
| Componentes en capas desplegados | ✅ | — | — |
| Primer flujo de punta a punta | ✅ compra → QR → validación | — | — |
| Seguridad base: **autenticación con JWT** (backend) | ✅ `POST /api/auth/login` + Bearer (en revisión) | [PAS-33](https://linear.app/passly/issue/PAS-33) | Juan |
| Seguridad base: **JWT en la app web** | ❌ el front guarda el password en `sessionStorage` | [PAS-34](https://linear.app/passly/issue/PAS-34) | Juan |
| Control de acceso por rol | ✅ `@PreAuthorize` + membresía por productora | — | — |
| **Patrón State** en la entidad central, **409** ante transición inválida | 🟡 hay 409, pero con enum + `if`, no con State. No existe "cancelar" | [PAS-35](https://linear.app/passly/issue/PAS-35) | Simón |
| Patrones implementados **cada uno con su ADR** | ❌ solo hay 2 ADR, y son de arquitectura | [PAS-36](https://linear.app/passly/issue/PAS-36) | Simón |
| Mensajería funcionando en al menos un proceso | ✅ cola `orden.pagada` (PAS-32) | — | — |
| Primeros tests unitarios | 🟡 15 clases de test, casi todas de integración | [PAS-38](https://linear.app/passly/issue/PAS-38) | Lucio |
| Sin defectos conocidos sin registrar | ❌ se pueden comprar entradas de un **borrador** | [PAS-37](https://linear.app/passly/issue/PAS-37) | Lucio |
| **Jira** al día | ❓ el equipo usa Linear; confirmar si la cátedra lo acepta | [PAS-39](https://linear.app/passly/issue/PAS-39) | Fede |
| Feedback del diseño incorporado | ❓ no está registrado en ningún lado | [PAS-39](https://linear.app/passly/issue/PAS-39) | Fede |
| Demo en vivo + documentación + declaración de IA | ❌ | [PAS-40](https://linear.app/passly/issue/PAS-40) | Fede |

**Orden sugerido:** PAS-33 antes que PAS-34 y PAS-40 (los `.http` y el guion dependen del
JWT). PAS-35 antes que el ADR de State de PAS-36. PAS-37 puede usar el `admiteVentas()` de
PAS-35 si llega a tiempo. **Ensayo completo el 15/11 a la noche, sobre `main` y desde una base
limpia.**

---

## 2. 3er Checkpoint — 30/11

Sin issues todavía: se cargan después del 16/11, con las observaciones del profe. **Todo el
equipo tiene que leer el PDF de Criterios de Testing**, no solo el QA.

| Requisito | Estado hoy | Notas |
|---|---|---|
| **SOAP** con el sistema legado (AFIP) | ❌ solo el puerto `ClienteAfip` con un adapter que escribe en el log | Spring WS + JAXB contra un mock con WSDL. Facturas hoy en `PENDIENTE_AFIP` |
| SOAP: manejo de **Fault** | ❌ | Mapear el Fault a un estado de factura y reintento |
| SOAP: protección contra **XXE** | ❌ | Parser con DTD y entidades externas deshabilitadas, con un test que lo demuestre |
| REST con el sistema moderno | ✅ pasarela de pago (PAS-7, PAS-29) | |
| Mensajería completa | 🟡 cola sí; **tópico `ticket.emitido` no** | Suscriptores: Notificaciones (mail con entradas, HU-13) y Accesos |
| Resiliencia: **timeout de 5 s con fallback** | 🟡 la pasarela tiene 5 s de lectura y 3 s de conexión, pero no hay fallback: solo 503 | Definir el fallback (por ejemplo, circuit breaker con mensaje claro y carrito intacto) |
| Consumidores idempotentes y **DLQ** | ✅ en `orden.pagada` | Repetirlo en el consumidor del tópico |
| **Si se apaga el broker no se pierden mensajes** | ❌ sin outbox: si el broker cae al publicar, el mensaje se pierde | Outbox o Event Publication Registry de Spring Modulith |
| **Idempotency-Key** en la API REST | 🟡 solo en la compra como invitado | Extender a la compra autenticada |
| **ETag** en la API REST | ❌ | Por ejemplo, GET de evento con `ETag` y `If-Match` en la edición (ya hay `@Version`) |
| Errores en formato **Problem Details** | ✅ en general | Revisar que no quede ningún 500 sin `ProblemDetail` |
| **Funcionalidad de IA** a través de su gateway | ❌ no hay nada | Definir la funcionalidad y el gateway. Pesa 10 % del final (eje E) |
| **QA designado** en el tablero | ❌ | Se propone en PAS-39 |
| **Pipeline de CI** con reporte de cobertura parcial | 🟡 el CI corre un solo test | Se arranca en PAS-38 |
| Plan de pruebas (1.ª versión) | ❌ | |
| Matriz de trazabilidad (1.ª versión) | ❌ | Historias → casos de prueba → tests. Se arranca en PAS-39 |
| Registro de defectos (1.ª versión) | ❌ | `docs/defectos.md`, se arranca en PAS-37 |
| Manual de usuario (1.ª versión) | ✅ `docs/manual-de-usuario.md` | Actualizar con JWT, cancelación e IA |
| **Ensayo de G1 a G9** | ❌ | Mostrar cómo se va a cumplir cada una y qué falta |

---

## 3. Entrega final — 07/12

### Condiciones G1 a G9 (eliminatorias: con una sola que falle, se desaprueba)

| | Condición | Estado hoy |
|---|---|---|
| G | Build reproducible con **un solo comando** | ❌ hoy son tres pasos (compose, backend, frontend) |
| G | Tag **`v1.0-final`** en `main` antes del turno | ❌ no hay tags |
| G | Pipeline de **CI en verde** | 🟡 (PAS-38) |
| G | **Jira** al día | ❓ (PAS-39) |
| G | **Sin secretos** en el historial | ✅ revisión rápida sin hallazgos. Correr `gitleaks` antes del final |
| G | **Demo en vivo** | — |
| G | **Declaración de uso de IA** | ❌ para esta etapa |
| G | **Informe honesto** | ❌ |

> La numeración exacta G1 a G9 está en el PDF de Criterios de Testing: completar esta tabla con
> ese PDF.

### Ejes A a E (mínimo 4 en **cada** eje)

| Eje | Peso | Qué se mide (ejemplos del mensaje) | Estado hoy |
|---|---|---|---|
| A — Historias de usuario y documentación | 25 % | Documentación completa | 🟡 |
| B — Código y tests | 25 % | Cobertura de negocio **≥ 80 % líneas / ≥ 70 % ramas**, **mutation ≥ 60 %**, **Quality Gate de Sonar** | ❌ sin JaCoCo, PIT ni Sonar |
| C — Integración | 20 % | SOAP, REST y mensajería con resiliencia; **carga p95 < 500 ms** | 🟡 / ❌ sin prueba de carga |
| D — Seguridad | 20 % | BOLA/IDOR, BFLA, **JWT inválidos**, inyecciones, **429 ante fuerza bruta**, **ZAP sin alertas High** | 🟡 IDOR de órdenes ya responde 404; ❌ sin 429 en login ni ZAP |
| E — IA | 10 % | Testing de la funcionalidad de IA | ❌ |
| Transversal | — | Responsive y **Lighthouse accesibilidad ≥ 90** | ❓ responsive comprobado a 320 y 390 px; Lighthouse sin medir |
| Transversal | — | **Ningún defecto Bloqueante ni Crítico abierto**, máximo 3 Mayores con workaround | ❓ |

### Otros pendientes del producto (si están entre las historias comprometidas)

- App móvil del validador (React Native). Hoy es la pantalla web `/validar`.
- Reintegros y aviso a compradores al cancelar un evento (HU-14, después de PAS-35).
- Mail con entradas para compradores con cuenta (HU-13, llega con el tópico).

### Deuda técnica que conviene cerrar antes del 07/12

- `ddl-auto: update` → Flyway o `validate`. Ayuda al build reproducible.
- Compensación del cobro best-effort (CLAUDE.md §10).
- Rotación de `JSESSIONID`: con JWT sigue rotando (verificado en PAS-33); PAS-34 decide cola serial o `sessionFixation().none()`.

---

## 4. Para aprobar

- Asistencia y exposición en el horario del grupo.
- Documentación y presentación: sin ellas no se aprueba.
- La nota es **individual**: cada uno tiene que poder explicar cualquier parte del sistema, del
  código o de **los tests que escribió**.
- Un defecto crítico que encuentre el profe descuenta más que uno que hayamos reportado
  nosotros: **registrar los defectos apenas se encuentran**.
- IA generativa: declararla. Un "CUMPLE" dictaminado por IA sin evidencia cuenta como NO
  VERIFICABLE.
