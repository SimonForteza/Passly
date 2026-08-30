# CLAUDE.md — Passly

> Este archivo es la memoria persistente del proyecto para Claude Code: se lee al
> inicio de cada sesión. Mantenerlo actualizado cuando se tome una decisión de
> arquitectura. El detalle largo va en `docs/`; acá queda solo lo esencial.

## Qué es Passly

Plataforma de **venta y validación de tickets para eventos** (tipo Passline).
Un comprador compra entradas online; el sistema cobra, emite un ticket con QR
único firmado, y en la puerta del evento un validador escanea el QR y lo marca
como usado (sin permitir doble uso).

Es el **TP Integrador de Desarrollo de Aplicaciones II** (comisión Lunes TM,
2.º cuatrimestre 2026). No es solo programar: es tomar y poder **defender
oralmente** decisiones de arquitectura. Cada integrante debe poder explicar
cualquier parte del sistema, no solo la que programó.

## Forma del sistema (cómo queda corriendo)

Passly son un **backend** y **dos clientes** que se hablan entre sí:

- **Backend — el corazón del TP (~80% de la nota).** Los 9 componentes Java
  sobre WildFly. **No es una página web:** es un servidor de aplicaciones que
  expone *APIs* (REST + un SOAP). Pura lógica, sin pantallas.
- **App web — el sistema principal.** La usan el **comprador** (compra entradas)
  y el **organizador** (administra sus eventos y ve reportes de ventas). Es el
  medio para **demostrar** que el backend funciona; lo visual suma puntos extra
  (temas claro/oscuro, buen diseño) pero **no** es donde se juega la materia.
- **App móvil — solo el validador.** Escanea los QR de las entradas en la puerta
  del evento y las marca como usadas. El escaneo de QR en puerta es un uso natural
  de mobile (cámara del teléfono).

Cuando prende todo, el sistema es:

- una **base de datos relacional** con los datos;
- **WildFly** corriendo los 9 componentes, con **Artemis** adentro (colas y
  tópicos), exponiendo **REST** (a los dos clientes y a la pasarela) y **SOAP con
  WSDL** (al "AFIP" legado, mockeado);
- la **app web** para comprador y organizador;
- la **app móvil del validador** para el escaneo de QR en puerta;
- dos **sistemas externos**: pasarela de pago (REST) y AFIP (SOAP simulado).

**Recorrido end-to-end** (este único flujo toca casi todo el checklist):
el comprador elige entradas **desde la app web** → la web llama por REST a
`ServicioDeVentas` → hold en inventario + cobro por la pasarela (REST) → al
confirmarse el pago se encola "emitir tickets" → un worker genera el QR, factura
contra AFIP (SOAP) y publica `TicketEmitido` → Notificaciones manda el mail. En
el evento, la **app móvil del validador** escanea el QR y `ServicioDeValidacion`
marca la entrada como usada.

## Lenguajes y datos

- **Java** — todo el backend (Jakarta EE). El grueso del trabajo y de la nota.
- **SQL / base relacional** — PostgreSQL o MySQL. Pero con **JPA (Hibernate, ya
  incluido en WildFly)** casi no se escribe SQL a mano: se definen *entidades*
  Java (`Evento`, `Ticket`, `Orden`…) y el ORM genera las tablas y el SQL. Para
  consultar se usa **JPQL** (sobre objetos Java, no sobre tablas). SQL crudo solo
  en el caso puntual donde JPA no alcanza.
- **App web** — **HTML/CSS/JS** (o un framework, a definir). Consume el backend
  por REST.
- **App móvil del validador** — **su propio stack (a definir)**. Consume el
  backend por REST; para escanear QR usa la cámara.
- **XML** — configuración del backend y el **WSDL** del servicio SOAP (el
  contrato de AFIP se describe en XML).

## Stack y entorno

- **Backend:** Jakarta EE sobre **WildFly (EE10)**. Empaquetado **WAR**.
- **Build:** Maven 3.9.x. **JDK** por DNF.
- **Base de datos:** relacional (PostgreSQL o MySQL) vía **JPA / Hibernate**.
- **Mensajería:** JMS con **Artemis embebido en WildFly** (no hace falta broker aparte).
- **Seguridad:** Jakarta Security / Elytron con `@RolesAllowed`.
- **App web:** HTML/CSS/JS o framework (a definir). Consume el backend por **REST**.
- **App móvil (validador):** stack a definir. Consume el backend por **REST**;
  para escanear QR usa la cámara.
- (Ajustar versiones exactas según el `pom.xml` real y el build de cada cliente.)

### Entorno local (Fedora, dual-boot)

- Herramientas pesadas y proyectos viven en el **HDD ext4 en `/mnt/hdd`**; el
  SSD queda para el sistema.
- Repo local de Maven **redirigido a `/mnt/hdd/maven-repo`** vía `~/.m2/settings.xml`.
- WildFly y el proyecto viven bajo `/mnt/hdd/`.
- **VS Code es Flatpak** (PATH aislado). La terminal usa un perfil con
  `flatpak-spawn --host bash` para ejecutar comandos en el host real. El warning
  `ioctl` que aparece es inofensivo.

> Estas rutas son personales de mi máquina: si esto se comparte con el equipo,
> mover lo específico de mi entorno a `CLAUDE.local.md` (gitignored).

## Arquitectura

### Regla de oro

**Rebanadas verticales, no capas horizontales.** Se termina un componente
completo (sus 3 capas, desplegado y andando) antes de arrancar el siguiente. La
unidad de avance es "un componente que funciona", no "una capa a medias en todos".

**Todo tiene que correr en vivo.** En cada entrega se muestra la app funcionando,
no diagramas ni código sin ejecutar. Nada de demos falseadas.

### Arquitectura en capas (en cada componente)

`presentación (JAX-RS / JAX-WS)` → `negocio (EJB)` → `datos (DAO + JPA)`

Las tres capas separadas y explícitas. El esqueleto de referencia es el
componente base: `Resource` (recurso) → `Service` (EJB) → `DAO`.

### Componentes (9 — mínimo pedido: 6)

| Componente | Responsabilidad | Estado |
|---|---|---|
| ServicioDeUsuarios | Registro/login de compradores, organizadores, validadores; roles | stateless |
| ServicioDeEventos | Alta/gestión de eventos: fecha, lugar, tipos de entrada, cupos, precios | stateless |
| **ServicioDeInventario** | Stock de entradas por sector/tipo; **hold temporal (~10 min)** mientras el comprador paga | **stateful** |
| ServicioDeVentas | Orquesta la compra (reserva → cobro → emisión). Es el **Facade** | stateless |
| ServicioDePagos | Integración REST con pasarela moderna (Mercado Pago/Stripe) | stateless |
| ServicioDeFacturacion | Integración SOAP con AFIP (facturación electrónica) | stateless |
| ServicioDeTickets | Genera el ticket con QR único firmado tras confirmar pago | stateless |
| ServicioDeValidacion | Valida el QR en puerta y marca la entrada como usada (evita doble uso) | stateless |
| ServicioDeNotificaciones | Envía el ticket por mail, recordatorios, avisos (multicanal) | stateless |

El **stateful obligatorio** es `ServicioDeInventario`, justificado por el hold
temporal de entradas (mismo patrón que `ServicioDeTurnos` en el caso MediConecta
del TP). El resto son stateless.

### Patrones de diseño (mínimo: 3 distintos, justificados)

- **Facade** → `ServicioDeVentas` esconde inventario + pago + emisión + facturación.
- **Adapter** → interfaz común de "proveedor externo" sobre AFIP (SOAP) y pasarela (REST).
- **DAO** → acceso a datos desacoplado de la lógica en todos los componentes.
- **Strategy** (reserva) → políticas de precio intercambiables (early-bird, dinámico, descuentos).
- **Factory** (reserva) → creación de tickets/facturas según tipo de entrada.

> "Reserva" = candidatos extra por si se descarta alguno. Cada patrón hay que
> saber justificar **qué problema resuelve** y **por qué se descartaron alternativas**.

### Integraciones externas

- **SOAP legado (con WSDL):** AFIP. En homologación conviene **mockear** el
  endpoint SOAP en vez de pelear con certificados de producción.
- **REST moderno:** pasarela de pago + la API REST del backend, consumida por la
  **app web** (comprador/organizador) y la **app móvil del validador**.
- **Cola punto a punto (P2P):** al confirmarse el pago se encola "emitir tickets
  de la orden X"; **un solo** worker genera los QR y persiste.
- **Tópico pub/sub:** al emitirse el ticket se publica `TicketEmitido`, que
  reciben **varios** suscriptores a la vez (Notificaciones, Analítica…).

### Seguridad y transacciones

- **Roles:** `COMPRADOR`, `ORGANIZADOR`, `VALIDADOR`, `ADMIN`.
- **Operaciones sensibles (≥2):** solo el ORGANIZADOR crea/edita su evento; solo
  el VALIDADOR marca tickets usados; solo el COMPRADOR dueño descarga su entrada.
- **Transacción declarativa** (`@Transactional`) en el flujo crítico
  **reservar hold → cobrar → emitir → facturar**. Si falla un paso, se libera el hold.

## Requisitos técnicos transversales (checklist §6 — la vara de evaluación)

- [ ] 6+ componentes de negocio, cada uno con interfaz explícita y documentada
- [ ] 1 stateful + 1 stateless, ambos justificados
- [ ] Arquitectura en capas explícita en cada componente
- [ ] 3+ patrones de diseño distintos, aplicados y justificados
- [ ] 1 integración SOAP con WSDL (sistema legado) → AFIP
- [ ] 1 integración REST (partner moderno / API externa) → pasarela
- [ ] 2 procesos async: 1 cola P2P + 1 tópico pub/sub
- [ ] Seguridad declarativa (auth + rol) en 2+ operaciones sensibles
- [ ] Transacciones declarativas en 1 flujo crítico multipaso
- [ ] Stack consistente y justificado (Jakarta EE)
- [ ] Repo Git con historial incremental (no un volcado final)

## Cronograma de entregas

Regla del §4 del anexo: **cada entrega suma funcionalidad real y verificable
sobre la anterior**. No se puede mostrar menos que en la entrega previa.

| # | Fecha | Carácter | Qué se entrega (resumen) |
|---|---|---|---|
| Checkpoint 1 | **31/08** | Fecha NO obligatoria (formativa) | 6+ componentes identificados con interfaces + diagrama de arquitectura + stack justificado + **1 componente desplegado en capas, corriendo** + doc 2-4 pág |
| Obligatoria 1 | **14/09** | Obligatoria, defensa oral | 3+ componentes andando + 1 stateful/1 stateless + 3 patrones + seguridad por rol + doc 5-8 pág |
| Checkpoint 2 | **19/10** | Fecha NO obligatoria (formativa) | Diagrama de integración (SOA) + 1 proceso async real + **doc funcional + manual de usuario + prototipo Figma** |
| Obligatoria 2 | **09/11** | Obligatoria, defensa oral | 2 async (cola + tópico) + SOAP/AFIP + REST + transacciones + doc 8-12 pág con diagramas de secuencia |
| Prueba individual | a confirmar | Verificación individual | Cada integrante responde sobre cualquier parte del sistema |
| Final | **21/12 o 30/11** (confirmar) | Obligatoria, defensa oral | Sistema completo integrado punta a punta + doc 10-15 pág + repo con historial de todos |

El detalle exhaustivo de cada entrega vive en `docs/entregas.md`.

## Convenciones de trabajo

- **Git:** commits incrementales y frecuentes, con participación visible de
  **todos** los integrantes. Nada de un único commit final.
- **Jira** (o Trello/Azure DevOps): tablero al día en **todo momento**, no la
  víspera. Acceso de lectura al docente desde la primera entrega. El docente
  puede pedir verlo sin aviso.
- **Documentación = subproducto del trabajo.** Los `.md` de `docs/` se escriben
  a medida que se decide, y en cada entrega se exportan a PDF. Es el 15% de
  "Calidad de documentación" de la rúbrica, hecho sin trabajo extra.
- **Declarar uso de IA** en el documento de cada entrega obligatoria: qué se usó
  y para qué. Cada integrante debe poder defender cualquier parte igual.
- **App web → responsive obligatorio.**
- **App móvil del validador → adaptarse a distintos tamaños de pantalla y a la
  orientación (vertical/horizontal).**
- Prototipo navegable en Figma para el 19/10.

## Estructura del repo

```
passly/
├── CLAUDE.md              ← este archivo (briefing del proyecto)
├── CLAUDE.local.md        ← rutas/entorno personales (gitignored)
├── README.md              ← qué es Passly y cómo levantarlo
├── docs/
│   ├── arquitectura.md    ← mapa de componentes y decisiones
│   ├── componentes.md     ← cada componente: interfaz, operaciones, responsabilidad
│   ├── patrones.md        ← qué patrón, dónde, por qué, alternativas descartadas
│   ├── integraciones.md   ← SOAP/REST/cola/tópico y justificación de cada canal
│   ├── entregas.md        ← detalle completo de cada entrega y su checklist
│   └── adr/               ← Architecture Decision Records (puntos extra §7)
├── src/                   ← backend: código (un módulo/paquete por componente)
├── web/                   ← app web (comprador + organizador; stack a definir)
└── app-validador/         ← app móvil del validador (escaneo de QR; stack a definir)
```

## Dudas abiertas (confirmar / decidir)

- **Tecnología de cada cliente:** el frontend ya está decidido (app web +
  app móvil del validador); queda por definir **qué tecnología** usa cada uno
  (framework de la web; stack de la app móvil).
- **Base de datos:** elegir PostgreSQL o MySQL.
- **Fecha de la Entrega Final:** la tabla del TP dice **21/12**; el detalle de esa
  misma entrega y todo el anexo dicen **30/11**. Confirmar con la cátedra.
- **Nota mínima de la final:** la tabla dice **mín. 4**; el detalle dice
  **mín. 8 → aprobación directa**. Confirmar.
- Confirmar que la comisión maneja las mismas fechas de checkpoints.
