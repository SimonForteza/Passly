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
| Seguridad base: **autenticación con JWT** (backend) | ✅ `POST /api/auth/login` + Bearer (integrado en `main`) | [PAS-33](https://linear.app/passly/issue/PAS-33) | Juan |
| Seguridad base: **JWT en la app web** | ✅ guarda solo el token; 401 → login (integrado en `main`) | [PAS-34](https://linear.app/passly/issue/PAS-34) | Juan |
| Control de acceso por rol | ✅ `@PreAuthorize` + membresía por productora | — | — |
| **Patrón State** en la entidad central, **409** ante transición inválida | ✅ `EstadoDeEvento` + una clase por estado; cancelar con 409 al repetir | [PAS-35](https://linear.app/passly/issue/PAS-35) | Lucio |
| Patrones implementados **cada uno con su ADR** | ✅ ADR-003 a ADR-007 (Facade, Adapter/Port, DAO, State, stateful), [índice](adr/README.md) | [PAS-36](https://linear.app/passly/issue/PAS-36) | Lucio |
| Mensajería funcionando en al menos un proceso | ✅ cola `orden.pagada` (PAS-32) | — | — |
| Primeros tests unitarios | ✅ 4 clases nuevas de unitarios puros de negocio (Ventas, Productoras, Accesos, Invitaciones; 47 casos), 11 unitarias en total; CI con la suite completa | [PAS-38](https://linear.app/passly/issue/PAS-38) | Simón |
| Sin defectos conocidos sin registrar | ✅ ~~se pueden comprar entradas de un **borrador**~~ corregido y registrado como DEF-001 | [PAS-37](https://linear.app/passly/issue/PAS-37) | Simón |
| **Jira** al día | ❓ Linear es el tablero operativo. Pendiente confirmación de la cátedra y acceso de lectura | [PAS-39](https://linear.app/passly/issue/PAS-39) | Fede |
| Feedback del diseño incorporado | ❓ Registro preparado. Pendiente relevar devoluciones de O1, C2 y Teams | [PAS-39](https://linear.app/passly/issue/PAS-39) | Fede |
| Demo en vivo + documentación + declaración de IA | 🟡 guion e informe listos para revisión. Ensayo y presentación pendientes | [PAS-40](https://linear.app/passly/issue/PAS-40) | Fede |

**Tablero:** [PAS-33 a PAS-40 con responsables y estados](trazabilidad.md#tablero-del-1611).
Falta confirmar por Teams si la cátedra acepta Linear con acceso de lectura. Si exige Jira, migrar
los issues y acordar con el equipo el cambio de tablero antes de dar acceso al docente. Registrar
aquí la respuesta y cuándo se verificó el acceso. Para QA al 30/11 se propone a **Simón Forteza**,
responsable de PAS-38. La designación queda pendiente de confirmación del equipo en el tablero oficial.

**Diseño:** [matriz de historias](trazabilidad.md) y [registro de feedback](feedback-diseno.md).
La HU-13 tiene seguimiento en [PAS-41](https://linear.app/passly/issue/PAS-41).

**Ensayo y presentación:** [guion](entrega-2/guion-demo.md) e [informe PDF](entrega-2/informe-segunda-entrega.pdf).
Ensayo grupal previsto para el 15/11 a la noche sobre `main` y una base descartable. PAS-35 ya integró
la cancelación y el ADR de State. Tras el ensayo, agregar fecha, asistentes, commit y defectos.
Después de exponer el 16/11, registrar el resultado y las observaciones para el 30/11.

---

## 2. 3er Checkpoint — 30/11

Los issues restantes se cargan después del 16/11, con las observaciones del profe. **Todo el
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
| **QA designado** en el tablero | ❌ | Simón Forteza propuesto en PAS-39. Falta confirmación y designación en el tablero oficial. |
| **Pipeline de CI** con reporte de cobertura parcial | ✅ `ci.yml`: suite completa contra Postgres + JaCoCo como artefacto + lint/build del front (PAS-38) | Falta fijar el umbral que rompe el build |
| Plan de pruebas (1.ª versión) | ❌ | |
| Matriz de trazabilidad (1.ª versión) | 🟡 | [Historias → issues](trazabilidad.md). Completar casos de prueba y resultados. |
| Registro de defectos (1.ª versión) | ✅ [`docs/defectos.md`](defectos.md) con DEF-001 (PAS-37) | Seguir registrando cada defecto apenas se detecta |
| Manual de usuario (1.ª versión) | ✅ `docs/manual-de-usuario.md` | Actualizar con JWT, cancelación e IA |
| **Ensayo de G1 a G9** | ❌ | Mostrar cómo se va a cumplir cada una y qué falta |

---

## 3. Entrega final — 07/12

### Condiciones G1 a G9 (eliminatorias: con una sola que falle, se desaprueba)

| | Condición | Estado hoy |
|---|---|---|
| G | Build reproducible con **un solo comando** | ❌ hoy son tres pasos (compose, backend, frontend) |
| G | Tag **`v1.0-final`** en `main` antes del turno | ❌ no hay tags |
| G | Pipeline de **CI en verde** | ✅ `ci.yml` (PAS-38) |
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
| B — Código y tests | 25 % | Cobertura de negocio **≥ 80 % líneas / ≥ 70 % ramas**, **mutation ≥ 60 %**, **Quality Gate de Sonar** | 🟡 JaCoCo listo (PAS-38). Cobertura de negocio al 15/11: **suite completa 94,8 % líneas / 80,8 % ramas**; **solo unitarios 32,9 % / 31,2 %** (lo demás lo cubren los tests de integración). Faltan PIT y Sonar |
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
- Rotación de `JSESSIONID`: con JWT sigue rotando (verificado en PAS-33); PAS-34 mantuvo la cola serial del frontend; `sessionFixation().none()` queda como decisión del equipo.

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
