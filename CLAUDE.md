# CLAUDE.md — Passly

> Memoria persistente del proyecto. Se lee al inicio de cada sesión de Claude Code.
> Actualizar cuando se tome una decisión de arquitectura. El detalle largo va en
> `docs/`; acá queda solo lo esencial y las razones.

---

## 1. Qué es Passly

Plataforma de **venta y validación de entradas para eventos** (tipo Passline).
Un comprador adquiere entradas online, el sistema cobra a través de una pasarela
externa, emite un ticket con **QR único firmado criptográficamente**, y en la
puerta del evento un validador escanea ese QR y lo marca como usado, impidiendo
todo doble uso.

Es el **TP Integrador de Desarrollo de Aplicaciones II** (3.4.218, comisión
Lunes TM, 2.º cuatrimestre 2026).

**Lo que se evalúa no es la cantidad de código sino la calidad de las decisiones
de arquitectura y la capacidad de defenderlas oralmente.** Cada integrante debe
poder explicar cualquier parte del sistema, no solo la que programó. Un sistema
técnicamente simple pero bien justificado rinde mejor que uno más grande con
decisiones improvisadas.

### Enlaces

| Recurso | URL |
|---|---|
| **Repositorio** | https://github.com/SimonForteza/Passly |
| Spring Boot — documentación | https://docs.spring.io/spring-boot/index.html |
| Spring Modulith — documentación | https://docs.spring.io/spring-modulith/reference/index.html |
| Supabase — documentación | https://supabase.com/docs |

> Ante una duda de API, configuración o versión, consultar la documentación
> oficial antes de asumir. Spring Modulith y Supabase evolucionan rápido y el
> conocimiento previo puede estar desactualizado.

---

## 2. Forma del sistema

Passly es **un backend y dos clientes**.

- **Backend — el corazón del TP.** Ocho componentes Java sobre Spring Boot.
  No es una página web: es un servidor que expone APIs (REST + un SOAP).
  Lógica pura, sin pantallas.
- **App web** — la usan el **comprador** (compra entradas) y el **organizador**
  (administra eventos y ve reportes). Es el medio para demostrar que el backend
  funciona. Lo visual suma, pero no es donde se juega la nota.
- **App móvil** — solo el **validador**. Escanea QR en la puerta del evento.
  El uso de la cámara justifica naturalmente que sea mobile y no web.

### Recorrido end-to-end

Este único flujo toca casi todo el checklist y es el que se demuestra en vivo:

```
App web → REST → ServicioDeVentas
  ├─ carrito temporal de entradas (componente stateful)
  ├─ cobro vía pasarela de pago (REST saliente)
  └─ [TRANSACCIÓN DECLARATIVA]
       descuento de cupo → registro de la orden → emisión de tickets
            │
            ├─ publica en COLA P2P  → ServicioDeFacturacion → AFIP (SOAP)
            └─ publica en TÓPICO    → ServicioDeNotificaciones (mail)
                                    → ServicioDeAccesos (precarga validación)

En el evento: App móvil → REST → ServicioDeAccesos → marca el ticket como usado
```

**Detalle importante para la defensa:** la facturación contra AFIP queda
**fuera** de la transacción, a propósito. Meter una llamada a un sistema externo
lento dentro de un `@Transactional` mantiene la fila de la base bloqueada y
además no es rollbackeable. Por eso la venta se confirma en la transacción y la
factura sale por cola, con reintento. Si preguntan "¿qué pasa si AFIP está caído
a mitad del flujo?", la respuesta es que la venta ya está confirmada y el mensaje
se reintenta desde la cola.

---

## 3. Stack

| Necesidad | Tecnología | Por qué |
|---|---|---|
| Plataforma | **Spring Boot 4.1.1 / Java 21** | Habilitado explícitamente por la cátedra. Su contenedor administra ciclo de vida, scopes, transacciones y seguridad de forma declarativa |
| Modularización | **Spring Modulith 2.1.1** | Verifica automáticamente las fronteras entre componentes y genera diagramas desde el código |
| Persistencia | **Supabase (PostgreSQL)** | Postgres gestionado, compatible con JPA, con soporte nativo de esquemas |
| Acceso a datos | **Spring Data JPA + Hibernate** | Patrón DAO y transacciones declarativas |
| Mensajería | **ActiveMQ Artemis (Docker)** | Broker JMS nativo, alineado con la Unidad V |
| SOAP | **Spring Web Services + JAXB** | Generación y consumo de contratos WSDL |
| Seguridad | **Spring Security** | Autorización por rol declarativa |
| Build | **Maven** | — |
| App web | **React + Vite** (a confirmar) | Consume el backend por REST. Responsive obligatorio |
| App móvil | **React Native** (a confirmar) | Cámara para escaneo de QR. Debe adaptarse a distintos tamaños y orientaciones |

**Nota de versión (post-31/08):** el scaffold inicial se armó sobre Spring Boot 4.1.1 /
Spring Modulith 2.1.1. Es la combinación correcta según la matriz de compatibilidad oficial
(Modulith 2.x está alineado con Boot 4.x; Modulith 1.4.x lo está con Boot 3.5.x, no con 3.x en
general), pero difiere de lo que decía esta tabla en el informe entregado el 31/08. Si lo
preguntan: la versión se verificó contra la documentación oficial antes de fijarla, no se
copió de memoria, y el par 4.1.1 / 2.1.1 es consistente entre sí.

**Por qué Artemis y no RabbitMQ:** la Unidad V es JMS específicamente. Artemis
y ActiveMQ Classic hablan JMS nativo; RabbitMQ implementa AMQP y obligaría a una
capa de compatibilidad que después hay que explicar. Levantarlo en Docker (y no
embebido) es más defendible: la consigna pide mostrar el broker configurado y
funcionando.

### Trampas conocidas de Supabase

- **Conectarse al puerto 5432, no al 6543.** El 6543 es PgBouncer en modo
  transacción, que rompe prepared statements de JPA y puede romper las
  transacciones multipaso, que son justamente lo que hay que demostrar. Si
  aparecen errores raros, agregar `prepareThreshold=0` a la URL JDBC.
- **No usar la API REST autogenerada de Supabase (PostgREST) como "la integración
  REST".** Sería el frontend pegándole directo a la base, salteándose todos los
  componentes: la negación del TP. La integración REST es la pasarela de pago.
- **Supabase Auth no reemplaza la seguridad declarativa.** Puede usarse como
  proveedor de identidad, pero la autorización por rol tiene que estar en el
  backend con Spring Security y anotaciones, porque eso es lo que se evalúa.

---

## 4. Arquitectura

### 4.1 Regla de oro

**Rebanadas verticales, no capas horizontales.** Se termina un componente
completo (sus tres capas, desplegado y andando) antes de arrancar el siguiente.
La unidad de avance es "un componente que funciona", no "una capa a medias en
todos".

**Todo tiene que correr en vivo.** En cada entrega se muestra la app funcionando,
no diagramas ni código sin ejecutar. Nada de demos falseadas.

### 4.2 Paradigma: monolito modular, no monolito ni microservicios

El backend se despliega como **un único artefacto**, pero eso no lo hace un
monolito. Lo que define el paradigma es dónde están las fronteras y qué las hace
cumplir.

- **Monolito:** cualquier clase accede a los internos de cualquier otra; la
  separación en paquetes es convención sin mecanismo.
- **Monolito modular (lo nuestro):** cada componente publica una interfaz y
  encapsula su implementación, sus entidades y su acceso a datos. La frontera es
  el contrato, no el proceso.
- **Microservicios:** proceso autónomo por servicio, base privada, comunicación
  solo por red. Se gana autonomía de despliegue; se pierden las transacciones
  declarativas.

**Por qué no microservicios:** el flujo crítico (retener → cobrar → emitir) tiene
que ser transaccional, y la consigna exige transacciones declarativas en un flujo
multipaso. Distribuir en procesos obligaría a reemplazar eso por una saga con
compensación: todo el costo de una arquitectura distribuida sin ninguno de sus
beneficios. Este es un **ADR** a escribir.

### 4.3 Estructura de paquetes

Un paquete por componente. Lo público en la raíz, todo lo demás en `internal`.

Estructura **real** de `eventos`, que sirve de **plantilla** para los demás componentes. Los
internos están divididos en tres sub-paquetes que reflejan las capas de §4.5 —
`web` / `negocio` / `datos`:

```
com.passly/
├── PasslyApplication.java              ← arranque Spring Boot
├── eventos/                            ← componente de referencia (plantilla)
│   ├── EventoService.java              ← interfaz, public (el contrato)
│   ├── EstadoEvento.java               ← enum public (BORRADOR / PUBLICADO)
│   ├── EventoNoEncontradoException.java          ┐ excepciones públicas
│   ├── TipoEntradaNoEncontradoException.java      │ del contrato
│   ├── TransicionDeEstadoInvalidaException.java   ┘
│   ├── dto/
│   │   ├── package-info.java           ← @NamedInterface("dto")
│   │   ├── EventoDTO.java
│   │   ├── TipoEntradaDTO.java
│   │   ├── DisponibilidadDTO.java
│   │   ├── CrearEventoRequest.java
│   │   └── CrearTipoEntradaRequest.java
│   └── internal/
│       ├── CargaDeDatosDemo.java       package-private, @Profile("demo") — seeder
│       ├── negocio/
│       │   ├── EventoServiceImpl.java  package-private (movido acá en el refactor)
│       │   └── EventoMapper.java       package-private
│       ├── web/
│       │   ├── EventoController.java   package-private
│       │   └── ManejadorDeErrores.java package-private (@RestControllerAdvice)
│       └── datos/
│           ├── EventoRepository.java   public (ver nota de visibilidad)
│           ├── Evento.java             entidad JPA, public (ídem)
│           ├── TipoEntradaRepository.java  public
│           └── TipoEntrada.java        entidad JPA, public
```

> **Nota:** `EventoServiceImpl` ya no cuelga suelto de `internal/` — vive en
> `internal/negocio/` (commit `refactor(eventos): mover la capa de negocio a
> internal/negocio/`). Sigue siendo package-private; ver la nota de visibilidad
> más abajo sobre por qué eso obliga a que `datos/` sea `public`.

**Ya implementado con esta misma estructura:** `usuarios/` (PAS-5) — mismas capas
`web`/`negocio`/`datos`, DTOs con `@NamedInterface`, impl package-private. Suma además un
`@Bean PasswordEncoder` propio en `internal/negocio/` (BCrypt) para guardar la credencial
hasheada; ver §9. Y `productoras/` (PAS-7) — ídem, con `RolEnProductora` en la raíz como
parte del contrato.

**`ventas/` (PAS-8) — el componente stateful, con una variación real sobre la plantilla.**
Mismas capas, pero `internal/negocio/` suma dos colaboradores que `eventos` no necesita:

```
ventas/
├── VentaService.java                        ← interfaz, public (el Facade)
├── CarritoVencidoException.java             ┐
├── CarritoVacioException.java               │ excepciones públicas
├── CarritoDeOtroCompradorException.java     │ del contrato
├── PagoRechazadoException.java              │
├── OrdenNoEncontradaException.java          ┘
├── dto/                                     ← @NamedInterface("dto")
│   ├── CarritoDTO.java, ItemDeCarritoDTO.java, AgregarItemRequest.java
│   └── OrdenDTO.java, ItemDeOrdenDTO.java, CompradorDeOrdenDTO.java
└── internal/
    ├── negocio/
    │   ├── CarritoDeCompra.java     @Component @SessionScope — EL STATEFUL (§4.7)
    │   ├── VentaServiceImpl.java    package-private, SIN @Transactional de clase (§4.11)
    │   ├── ConfirmacionDeCompra.java  package-private, @Transactional acá (§4.11)
    │   ├── PasarelaDePago.java        interfaz package-private — el Port (§4.9)
    │   ├── PasarelaDePagoSimulada.java  el Adapter de hoy, hasta que exista PAS-7
    │   ├── ComprobanteDeCobroDTO.java  package-private, no cruza la frontera del módulo
    │   └── VentaMapper.java
    ├── web/
    │   ├── VentaController.java
    │   └── ManejadorDeErroresDeVentas.java
    └── datos/
        ├── Orden.java, ItemOrden.java     entidades JPA, public
        └── OrdenRepository.java
```

No depende de `productoras`: el nombre comercial del organizador ya viaja dentro de
`EventoDTO.organizador`, así que declarar esa arista sería sumar una dependencia sin uso. Su
`package-info` declara `{"eventos", "eventos :: dto", "usuarios", "usuarios :: dto"}`.

**Cada módulo suma un `package-info.java` en su raíz** con
`@ApplicationModule(allowedDependencies = ...)`, que declara de qué otros componentes puede
depender. No es decorativo: el build falla si aparece una dependencia no declarada (§4.4).

**Más un paquete que no es un componente de negocio:** `demo/`, con los datos de
demostración. Vive fuera de los módulos porque sembrar un evento de una productora exige
actuar como alguien de esa productora, y Eventos no puede consultar Usuarios — un seeder
adentro de Eventos habría necesitado justamente la dependencia que el diseño evita. Los
datos de demo son un **cliente** del sistema, como la app web, y usan solo contratos
públicos; que alcancen para operar todo el sistema prueba que esos contratos están
completos.

**Aspiracional — todavía no existen** (se crean rebanada por rebanada, §4.1):
`pagos/`, `tickets/`, `accesos/`, `notificaciones/`, `facturacion/`.

**Reglas duras:**

1. `EventoServiceImpl` es **package-private**. Spring la instancia por reflexión
   igual; los demás componentes la inyectan por el tipo `EventoService` sin poder
   nombrar la clase concreta.
2. Las **entidades JPA nunca salen del componente**. Las fronteras se cruzan
   siempre con DTOs.
3. **Ningún join entre esquemas de distintos componentes.** Si Ventas necesita el
   precio de un evento, llama a `EventoService.consultarDisponibilidad()`.

**Corrección (post-31/08, tras implementar Eventos):** este árbol tenía dos imprecisiones.

- **`dto/` no es público por default.** Spring Modulith cierra los módulos por defecto: *todo*
  sub-paquete es interno, `dto/` incluido, salvo que se lo marque explícitamente con
  `@org.springframework.modulith.NamedInterface("dto")` en un `package-info.java`. Sin eso, el
  build falla el día que otro componente importe un DTO de Eventos. Con un solo componente
  implementado no se nota — por eso conviene ponerlo desde el primer módulo, no cuando ya duela.
- **`EventoRepository` y `Evento` no pueden ser package-private como decía la regla 1.** La
  visibilidad de paquete de Java **no es jerárquica**: `internal` e `internal.datos` son
  paquetes distintos y no se ven entre sí, así que `EventoServiceImpl` (en `internal`) no puede
  usar una clase package-private de `internal.datos`. Tienen que ser `public`. Eso no debilita
  la frontera: `public` ahí significa "visible dentro del artefacto", no "parte del contrato".
  Quien impone la frontera real es Spring Modulith (§4.4) — si otro componente importa
  `eventos.internal.datos.Evento`, el build falla igual, aunque el compilador de Java lo
  permita. Es un buen argumento para el oral, no un defecto a esconder.

### 4.4 Verificación de fronteras (Spring Modulith)

La visibilidad de Java cubre el acceso directo, pero no impide que alguien
declare un repositorio público por descuido ni evita ciclos de dependencia.
Spring Modulith cierra esa brecha:

```java
@Test
void verificarModulos() {
    ApplicationModules.of(PasslyApplication.class).verify();
}
```

Si un componente accede a los `internal` de otro, o si aparece un ciclo, **el
build falla**. Ese es el argumento fuerte en la defensa: *"la frontera no es una
convención del equipo, está verificada en el build."*

`Documenter` genera además el diagrama de dependencias desde el código, lo que
mantiene la documentación sincronizada con la implementación real.

> **Estado: el andamiaje ya rinde, con cinco módulos.** El test vive en
> `backend/src/test/java/com/passly/EstructuraDeModulosTest.java` y corre verde
> (4 tests): `noHayViolacionesDeFrontera()`, `usuariosEsLaRaizDelGrafoDeDependencias()`,
> `ventasEsLaCimaDelGrafoYUsuariosLaRaiz()` y `generarDocumentacion()`, que emite los
> diagramas PlantUML/AsciiDoc (`target/spring-modulith-docs/`) con el `Documenter`.
>
> **Dependencias declaradas, no solo verificadas.** Cada módulo tiene un
> `package-info.java` con `@ApplicationModule(allowedDependencies = ...)`. Eventos
> declara `{"productoras", "productoras :: dto"}`, Productoras declara
> `{"usuarios", "usuarios :: dto"}`, Ventas declara
> `{"eventos", "eventos :: dto", "usuarios", "usuarios :: dto"}`, y **Usuarios declara
> `{}`** — que en Modulith significa *ninguna dependencia permitida*, no "sin
> restricciones" (verificado empíricamente; la documentación oficial no cubre ese
> caso). Así "Usuarios es la raíz" pasó de ser una afirmación del informe a una
> condición de compilación.
>
> **El episodio a contar en el oral — y la vez que fue distinta.** Al implementar
> múltiples productoras, Eventos pasó a necesitar Productoras. El build falló con
> `Module 'eventos' depends on ... Allowed targets: none` y siguió rojo hasta que la
> dependencia quedó declarada por escrito. Después falló
> `eventosEsLaRaizDelGrafoDeDependencias()`, que afirmaba algo que había dejado de
> ser cierto. Ninguna de las dos cosas dependió de que alguien las notara en una
> revisión de código. Es reproducible en vivo: comentar cualquiera de esas
> anotaciones y correr `./mvnw test`.
>
> Al agregar Ventas, en cambio, **el build no falló**: sus dependencias hacia Eventos
> y Usuarios ya estaban declaradas desde el primer commit del módulo. Lo que se rompió
> fue la *narrativa* — `elGrafoDeNegocioEsUnaCadena()` seguía afirmando una cadena de
> tres eslabones cuando el grafo ya era acíclico, con Ventas en la cima y dos caminos
> hacia Usuarios. Lo detectó una revisión del archivo, no un test rojo. Es la
> diferencia entre lo que Modulith impone (las aristas permitidas) y lo que sigue
> dependiendo del criterio del equipo (que la documentación describa el grafo real) —
> y vale contar las dos caras en la defensa, no solo la primera.

### 4.5 Capas dentro de cada componente

```
presentación (Controller + DTO) → negocio (Service) → datos (Repository + Entity)
```

- **Presentación:** traduce HTTP a llamadas sobre la interfaz de negocio.
  Sin lógica de dominio, sin conocer entidades.
- **Negocio:** reglas del dominio y límites transaccionales (`@Transactional`).
- **Datos:** patrón DAO sobre Spring Data JPA. Entidades mapeadas al esquema
  propio del componente.

### 4.6 Componentes (9 — mínimo pedido: 6)

| Componente / Interfaz | Responsabilidad | Estado |
|---|---|---|
| `ServicioDeUsuarios` / `UsuarioService` | Registro y autenticación de compradores, organizadores y validadores. Roles y credenciales | stateless |
| `ServicioDeProductoras` / `ProductoraService` | Identidad comercial de quien publica fiestas y el padrón de quiénes operan en su nombre | stateless |
| `ServicioDeEventos` / `EventoService` | Alta, edición y publicación de eventos. Tipos de entrada, precios y cupos | stateless |
| **`ServicioDeVentas` / `VentaService`** | Orquesta la compra. Mantiene el **carrito (~5 min)** mientras el comprador paga. Es el **Facade** — **implementado (PAS-8)** | **stateful** |
| `ServicioDePagos` / `PagoService` | Adapter REST hacia la pasarela de pago | stateless |
| `ServicioDeTickets` / `TicketService` | Emite tickets con QR firmado. Custodia el estado de uso | stateless |
| `ServicioDeAccesos` / `AccesoService` | Valida el QR en puerta y garantiza el uso único | stateless |
| `ServicioDeNotificaciones` / `NotificacionService` | Envío multicanal: ticket, recordatorios, cancelaciones | stateless |
| `ServicioDeFacturacion` / `FacturacionService` | Adapter SOAP hacia AFIP, con reintento ante rechazo | stateless |

**Grafo de dependencias:**

| Origen | Destino | Naturaleza |
|---|---|---|
| **Ventas** | **Eventos** | **Síncrona, implementada** — disponibilidad, descuento de cupo |
| **Ventas** | **Usuarios** | **Síncrona, implementada** — datos del comprador en la orden |
| Ventas | Pagos | Síncrona — cobro. Hoy resuelto **adentro** de Ventas con un Port/Adapter propio (`PasarelaDePago` / `PasarelaDePagoSimulada`, §4.9); si PAS-7 llega a existir como módulo aparte, esta fila pasa de "adapter interno" a "dependencia entre componentes" sin tocar `VentaServiceImpl` |
| Ventas | Tickets | Síncrona — emisión dentro de la transacción |
| Ventas | Facturación | **Asincrónica** — cola P2P `orden.pagada` |
| Tickets | Notificaciones, Accesos | **Asincrónica** — tópico `ticket.emitido` |
| Accesos | Tickets | Síncrona — verificación de firma y marcado |
| **Eventos** | **Productoras** | Síncrona — quién puede gestionar, y el nombre comercial para la cartelera |
| **Productoras** | **Usuarios** | Síncrona — identidad y rol global al incorporar un miembro |
| **Usuarios** | — | **Sin dependencias salientes** |

**`ServicioDeUsuarios` es la raíz del grafo.** El grafo de negocio implementado ya **no es
una cadena**: es un grafo acíclico con `ventas` en la cima, que llega a `usuarios` por dos
caminos — directo, y vía `eventos → productoras → usuarios` —, declarado en los
`package-info` y verificado en cada build (§4.4).

> **Corrección (post-14/09):** el informe del 31/08 decía que *Eventos* era la raíz
> y no tenía dependencias salientes. Era cierto mientras los eventos no tenían
> dueño. Al implementar múltiples productoras dejó de serlo, y lo relevante es que
> el cambio no pasó inadvertido: el build falló hasta declararlo. No es un error a
> esconder — es la mejor evidencia de que la verificación de fronteras sirve.

**Eventos no depende de Usuarios**, y eso es deliberado: la identidad queda detrás
de Productoras, que ya resolvió el cruce de roles al armar su padrón. Si Eventos
importara `com.passly.usuarios`, el build falla.

`ServicioDeVentas` concentra la mayor cantidad de dependencias salientes: es el
componente más complejo y el que más cuidado requiere para no volverse un punto
de acoplamiento excesivo.

**Por qué Productoras es un componente y no una tabla de Usuarios** (pregunta
probable en el oral): Usuarios custodia identidad y credenciales de personas;
Productora es identidad **comercial**, con ciclo de vida propio, nombre de
fantasía, CUIT y un padrón N:M. Sus consumidores futuros — Ventas, reportes,
facturación — no tienen nada que ver con el login. Meterla en Usuarios lo
volvería un god-module y le haría perder la raíz del grafo.

**Por qué Tickets y Accesos están separados** (pregunta probable en el oral):
operan sobre la misma entidad pero tienen responsabilidades distintas. Tickets es
propietario del ticket y su estado; Accesos resuelve el acto de validación en
puerta. La separación permite además que Accesos se suscriba al tópico para
precargar la información de validación, lo que no tendría sentido si fueran uno.

### 4.7 Estado: stateful vs stateless

**`ServicioDeVentas` es el stateful — implementado (PAS-8).** Durante la compra, el
carrito (`CarritoDeCompra`, `internal/negocio/`) vive en memoria mientras dura la
selección: crece llamada a llamada (agregar la misma línea dos veces suma cantidad,
no duplica), y expira a los 5 minutos desde que se creó. Es un
`@Component @SessionScope` — una instancia por sesión HTTP, administrada por el
contenedor — con **callbacks de ciclo de vida** (`@PostConstruct`, `@PreDestroy`)
logueados, que son la evidencia en vivo de que Spring decide cuándo nace y cuándo
muere, no el código de negocio.

**Todo lo demás es stateless.** Singletons de Spring, sin memoria del cliente
entre llamadas.

**Distinción crítica para la defensa:** *persistir datos no vuelve stateful a un
componente*. El estado relevante es el **conversacional en memoria**, no el del
negocio en disco. `ServicioDeFacturacion` puede guardar mil facturas y sigue
siendo stateless.

**Trade-off declarado por escrito:** el carrito podría persistirse en base o en
caché y volver el componente stateless — de hecho es lo que se hace en producción
para escalar. Se eligió la variante stateful deliberadamente para demostrar la
gestión de ciclo de vida por contenedor, contenido central de la Unidad II.
Reconocer el trade-off suma más que esconderlo.

**Dos relojes distintos, no uno redundante.** `expiraEn` es la regla de *negocio*:
absoluta desde que se creó el carrito, la valida el propio carrito en cada
operación de confirmar. El `server.servlet.session.timeout` de `application.yml`
(fijado en 5 min) es la limpieza del *contenedor*: decide cuándo Tomcat destruye la
sesión — y con ella, esta instancia —, no cuándo el negocio considera vencida la
oferta. Por eso el log de `@PreDestroy` por vencimiento puede aparecer bastante
después de que `expiraEn` ya pasó: el reaper de sesiones de Tomcat no corre al
segundo. `DELETE /api/ventas/carrito` invalida la sesión explícitamente para poder
mostrar el `@PreDestroy` en el acto durante la demo.

**La tensión con `SessionCreationPolicy.STATELESS` (PAS-6), y por qué no es una
contradicción.** Esa política solo le dice a *Spring Security* que no cree ni lea
`HttpSession` para guardar el `SecurityContext`: cada request se re-autentica con su
propio header Basic. No le dice nada al *servlet container*, que sigue creando una
sesión en cuanto algo la pide — y eso es exactamente lo que hace el proxy de un bean
`@SessionScope`. La autenticación sigue siendo stateless; el carrito, no.

**Consecuencia real, no cosmética, y la mitigación.** Con nada que asocie el
`JSESSIONID` al principal autenticado, alguien que reutilizara la cookie de sesión
de otro junto con sus propias credenciales Basic vería el carrito ajeno. El carrito
guarda el id del comprador en su primera operación y lo valida en cada llamada
siguiente (`CarritoDeOtroCompradorException` → 409) en vez de confiar en la sesión
a secas — barato, y convierte un riesgo real en una decisión explicada.

**Detalle no obvio para el oral, descubierto probando el flujo en vivo:** Spring
Security cambia el `JSESSIONID` en **cada** request autenticado
(`ChangeSessionIdAuthenticationStrategy`, protección contra *session fixation* —
corre incluso bajo `STATELESS`, porque solo mira si ya existe una sesión, no la
política de creación). El dato del carrito persiste porque el contenedor migra los
atributos de la sesión al nuevo id, pero la cookie de la respuesta anterior deja de
servir de inmediato. Un navegador real no lo nota (reenvía siempre la cookie más
reciente); probar el flujo a mano con `curl` sin encadenar `-b`/`-c` en *cada*
llamada lo hace ver como si el carrito se vaciara solo — no es un bug, es esta
rotación (ver `docs/http/README.md`).

### 4.8 Persistencia

**Una sola base física en Supabase, un esquema de Postgres por componente.**

```
passly (base)
├── usuarios.usuario
├── productoras.productora, productoras.miembro
├── eventos.evento, eventos.tipo_entrada
├── ventas.orden, ventas.item_orden
├── tickets.ticket
├── accesos.validacion
└── ...
```

En JPA: `@Table(name = "evento", schema = "eventos")`.

**Referencias entre esquemas: por id, sin clave foránea.** `eventos.evento.productora_id`
y `productoras.miembro.usuario_id` apuntan a tablas de otro componente y **no llevan FK
física**, porque una FK entre esquemas sería exactamente el join que esta sección prohíbe
y ataría los dos componentes a nivel de motor. La integridad la impone el negocio: antes
de crear un evento, Eventos le pregunta a `ProductoraService` si quien opera puede
gestionar esa productora — y esa pregunta da `false` para una productora inexistente, así
que no hay forma de crear un evento colgado de la nada. La contracara honesta es que un
borrado de productora podría dejar huérfanos; hoy ese borrado no existe, y queda declarado
como deuda junto a Flyway (§10).

**Por qué no una base por componente:** eliminaría las transacciones ACID locales
entre componentes y obligaría a sagas con compensación. Como el sistema se
despliega en una unidad única, sería asumir el costo completo de una arquitectura
distribuida sin ninguno de sus beneficios — el antipatrón **monolito distribuido**.
Este es el segundo **ADR** a escribir.

**En desarrollo se corre contra Postgres local en Docker, no contra Supabase directo**
(`docker-compose.yml`, esquema inicializado por `db/init/`). El componente no sabe contra qué
Postgres corre: la URL, el usuario y la contraseña salen de variables de entorno
(`PASSLY_DB_URL` / `PASSLY_DB_USER` / `PASSLY_DB_PASSWORD`, ver `.env.example`), con un default
que apunta al contenedor local. Pasar a Supabase es cambiar esas tres variables, nada de código.
Se eligió así para no depender de red durante el desarrollo diario y para no tener credenciales
reales dando vueltas; sigue valiendo el puerto 5432, nunca el 6543.

### 4.9 Patrones de diseño (mínimo: 3 distintos, justificados)

| Patrón | Dónde | Qué problema resuelve |
|---|---|---|
| **Facade** | `ServicioDeVentas` | Esconde la orquestación de inventario, pago, emisión y facturación tras una sola operación de compra |
| **Adapter** | `ServicioDePagos` (REST) y `ServicioDeFacturacion` (SOAP) | Interfaz común de "proveedor externo" sobre dos protocolos incompatibles |
| **DAO** | Capa de datos de todos los componentes | Desacopla la lógica del mecanismo de persistencia |
| **Strategy** *(reserva)* | Política de precios | Early-bird, general, cortesía, dinámico — intercambiables |
| **Factory** *(reserva)* | Creación de tickets/facturas | Según tipo de entrada |

> "Reserva" = candidatos extra por si se descarta alguno. De cada patrón hay que
> saber **qué problema resuelve** y **por qué se descartaron las alternativas**.

### 4.10 Integraciones

- **SOAP con WSDL (legado):** AFIP. Mockear el endpoint en vez de pelear con
  certificados de homologación.
- **REST (partner moderno):** pasarela de pago. Más la API REST propia que
  consumen la app web y la app móvil.
- **Cola punto a punto:** `orden.pagada` → Facturación. Un solo consumidor,
  porque una orden no puede facturarse dos veces. **Esa es la justificación de
  por qué es cola y no tópico.**
- **Tópico pub/sub:** `ticket.emitido` → Notificaciones + Accesos + métricas del
  organizador. Varios suscriptores independientes. **Esa es la justificación de
  por qué es tópico y no cola.**

### 4.11 Seguridad y transacciones

- **Roles:** `COMPRADOR`, `ORGANIZADOR`, `VALIDADOR`, `ADMIN`.
- **Autenticación (PAS-6):** **HTTP Basic**, sesión **STATELESS**, CSRF off (API REST).
  Vive en el módulo `seguridad/`, que autentica contra `usuarios` por el contrato
  `usuarios :: autenticacion` (`CredencialDTO`, que lleva el hash) y **reutiliza el mismo
  `@Bean PasswordEncoder` de `usuarios`** (inyectado por tipo, sin declarar un segundo bean).
  Se eligió Basic y no JWT: para la Obligatoria 1, JWT sería complejidad sin beneficio.
- **El `UserDetails` lleva el id numérico como username, no el email.** El login sigue siendo
  por email (es lo que el cliente manda en el header Basic), pero
  `DetalleDeUsuarioParaAutenticacion` arma el `UserDetails` con
  `User.withUsername(String.valueOf(credencial.idUsuario()))`. Como `Authentication#getName()`
  después del login devuelve ese username, cualquier controller de cualquier módulo obtiene el id
  de quien opera con un tipo de Spring Security — sin depender de `usuarios` para resolver
  email → id. Esto no es un detalle menor: `eventos` tiene declarado en su `package-info` que
  **no puede depender de `usuarios`** (§4.6), así que la resolución tiene que pasar por acá, una
  sola vez, dentro de `seguridad` (que sí depende de `usuarios` legítimamente).

**Los dos ejes de la autorización** (implementado). Rol y alcance responden preguntas
distintas y por eso son dos enums, no uno:

| | Pregunta | Dónde vive |
|---|---|---|
| `Rol` | *¿qué tipo de cosas puede hacer?* | `usuarios.usuario.rol` (uno solo por cuenta) |
| `RolEnProductora` | *¿sobre las fiestas de quién?* | `productoras.miembro.rol_interno` — `DUENIO` / `STAFF` / `VALIDADOR` |

Colapsarlos daría o un rol global por productora — perdiendo el sentido del rol — o un
único rol global, y entonces cualquier validador podría escanear los tickets de cualquier
productora.

La invariante: **los dos ejes se cruzan una sola vez, al incorporar al miembro** (`DUENIO`
y `STAFF` exigen `ORGANIZADOR`; `VALIDADOR` exige `VALIDADOR`). Después ninguna operación
vuelve a consultar el rol global: se pregunta por la membresía. La verificación cara se
paga en el alta, no en cada request. Esta invariante es la que hace posible el
`@PreAuthorize("hasRole('ORGANIZADOR')")` de más abajo sin excluir a nadie legítimo: todo
`DUENIO`/`STAFF` de una productora ya tiene el rol global `ORGANIZADOR`.

**`COMPRADOR` es implícito:** cualquier usuario autenticado puede comprar, y el rol solo
agrega capacidades por encima. Resuelve que un ORGANIZADOR también pueda comprar entradas
sin abrir una segunda cuenta. El costo declarado es que `COMPRADOR` queda casi decorativo;
la alternativa evaluada fue `@ElementCollection Set<Rol>`, descartada por no justificar el
cambio de contrato de `UsuarioDTO` en esta entrega.

- **Autorización por rol declarativa con `@PreAuthorize`, en los controllers.** Grano grueso
  (anónimo vs autenticado) en el `SecurityFilterChain`; el rol, en `@PreAuthorize` sobre cada
  operación sensible. Se puso en el controller y no en el servicio para no acoplar
  `eventos`/`usuarios` a Spring Security y para no romper los seeders de demo, que llaman al
  servicio directo al arrancar (un `@PreAuthorize` sobre el servicio los haría fallar con
  AccessDenied en el boot). **Implementado (2 ops + bonus):**
  1. **Eventos:** solo `ORGANIZADOR` crea (`POST /api/eventos`) y publica
     (`POST /api/eventos/{id}/publicacion`) — **y, por debajo, solo si además puede gestionar
     la productora dueña del evento** (`ProductoraService.puedeGestionarEventos`, PAS-13): el rol
     es el filtro grueso en el controller, la membresía es el filtro fino en el servicio.
  2. **Usuarios:** el alta pública (`POST /api/usuarios`) solo crea `COMPRADOR`;
     `ORGANIZADOR`/`VALIDADOR`/`ADMIN` los da de alta un `ADMIN` autenticado
     (`#solicitud.rol == COMPRADOR or hasRole('ADMIN')`). Cierra el pendiente de PAS-5: el
     rol viajaba libre en `CrearUsuarioRequest`.
  3. **Bonus:** `GET /api/usuarios` (listar) solo `ADMIN`.
- **Códigos de una denegación (comportamiento por defecto de Spring Security):** ante un
  `@PreAuthorize` que deniega, el status depende de si hay identidad. Principal **anónimo →
  401** (Spring invoca el `AuthenticationEntryPoint`: "identificate"); principal **autenticado
  sin el rol → 403**. Por eso, en el endpoint público `POST /api/usuarios`, un anónimo que pide
  un rol privilegiado recibe **401**, no 403 (el 403 aparece cuando ya está autenticado, p.ej.
  un `COMPRADOR` intentando crear un evento). Útil para el oral: *anon+deny = 401, auth+deny = 403*.
- **Autorización por dueño, no por rol — implementado (PAS-8), el tercer patrón de
  `@PreAuthorize` del sistema.** `COMPRADOR` es implícito (más arriba): cualquier
  autenticado compra, así que ningún endpoint de `ventas` necesita filtrar por rol en
  el `POST` de confirmar o de agregar al carrito — eso ya lo cubre
  `anyRequest().authenticated()`. Lo que sí exige autorización explícita es que un
  comprador solo vea *sus propias* órdenes:
  - `GET /api/ventas/ordenes/{id}` filtra por dueño **en el servicio**
    (`OrdenRepository.findByIdAndCompradorId`) y devuelve **404, no 403**, si la orden
    no es de quien pregunta. Contraste con `ProductoraNoGestionableException` (403):
    una productora es pública, en una cartelera; una orden ajena no debería siquiera
    confirmar que existe.
  - `GET /api/ventas/compradores/{idComprador}/ordenes` sí lleva `@PreAuthorize`, pero
    distinto de los dos que ya existían (puro rol en Eventos/Usuarios, rol + membresía
    en Eventos vs. Productoras): acá es **rol *o* identidad propia** —
    `hasRole('ADMIN') or #idComprador.equals(...authentication.name...)` — sin que el
    servicio conozca Spring Security.
- **Fuera de alcance de PAS-8:** las ops sensibles de `VALIDADOR` (marcar ticket usado)
  viven en un componente que todavía no existe (`ServicioDeAccesos`). Agregará su
  `@PreAuthorize` sobre la misma infra de autenticación.

> **Estado:** Eventos, Productoras y Ventas resuelven "quién opera" con la identidad de
> Spring Security (`Authentication#getName()`, ver arriba). Ningún endpoint del backend
> depende de un header falsificable.
- **Transacción declarativa — implementada (PAS-8).** `ConfirmacionDeCompra.confirmar`
  (`internal/negocio/` de `ventas`) es el límite transaccional real de confirmar
  compra: descuento de cupo (`EventoService.descontarCupo`, que se **une** a esta
  misma transacción por propagación `REQUIRED` — un solo commit para el `UPDATE` de
  `eventos.tipo_entrada` y los `INSERT` de `ventas.orden`/`ventas.item_orden`, en dos
  esquemas de la misma base física) → registro de la orden. Si un paso falla, se
  revierte todo. **El cobro queda afuera** de esa transacción (igual que la
  facturación en §2: un externo lento no puede mantener una fila bloqueada, y no es
  rollbackeable) — y por eso vive en `VentaServiceImpl`, que **no lleva
  `@Transactional` de clase**: si lo llevara, Spring abriría la transacción antes de
  que el código llegue a cobrar. El límite transaccional tiene que ser un salto entre
  beans (llamar a `this.metodo()` desde el mismo objeto no pasa por el proxy de
  Spring, y `@Transactional` no se aplicaría) — el motivo exacto está en el Javadoc de
  `ConfirmacionDeCompra`. Si la confirmación falla después de cobrar, se compensa
  (`PasarelaDePago.revertir`) y se relanza; el carrito no se vacía, para poder
  reintentar sin perder la selección.

---

## 5. Checklist §6 — la vara de evaluación

- [ ] 6+ componentes de negocio, cada uno con interfaz explícita y documentada
- [ ] 1 stateful + 1 stateless, ambos justificados por escrito
- [ ] Arquitectura en capas explícita en cada componente
- [ ] 3+ patrones de diseño distintos, aplicados y justificados
- [ ] 1 integración SOAP con WSDL (legado) → AFIP
- [ ] 1 integración REST (partner moderno) → pasarela de pago
- [ ] 2 procesos async: 1 cola P2P + 1 tópico pub/sub
- [ ] Seguridad declarativa (auth + rol) en 2+ operaciones sensibles
- [ ] Transacciones declarativas en 1 flujo crítico multipaso
- [ ] Stack consistente y justificado (Spring Boot)
- [ ] Repo Git con historial incremental, no un volcado final

### Puntos extra (§7)

- [ ] Resiliencia: si un externo cae, el resto sigue funcionando (Circuit Breaker opcional)
- [ ] Heterogeneidad: un componente en otra tecnología que igual se integra
- [ ] Escalabilidad: escalar un componente bajo carga y medir la mejora
- [ ] **2 ADR** — los dos ya identificados: (a) monolito modular vs. microservicios,
  (b) esquema por componente vs. base por componente

---

## 6. Cronograma

**Regla del §4 del anexo:** cada entrega suma funcionalidad real y verificable
sobre la anterior. No se puede mostrar menos que en la entrega previa.

| # | Fecha | Carácter | Qué se entrega |
|---|---|---|---|
| Checkpoint 1 | **31/08** | Formativo, no obligatorio | 6+ componentes con interfaces + diagrama + stack justificado + **1 componente desplegado y corriendo** + doc 2-4 pág |
| Obligatoria 1 | **14/09** | Obligatoria, defensa oral | 3+ componentes andando + 1 stateful/1 stateless + 3 patrones + seguridad por rol + doc 5-8 pág |
| Checkpoint 2 | **19/10** | Formativo, no obligatorio | Diagrama SOA + 1 proceso async real + doc funcional + manual de usuario + **prototipo Figma** |
| Obligatoria 2 | **09/11** | Obligatoria, defensa oral | 2 async (cola + tópico) + SOAP/AFIP + REST + transacciones + doc 8-12 pág con diagramas de secuencia |
| Prueba individual | a confirmar | Verificación individual | Cada integrante responde sobre cualquier parte del sistema |
| Final | **21/12 o 30/11** (confirmar) | Obligatoria, defensa oral | Sistema completo punta a punta + doc 10-15 pág + repo con historial de todos |

Detalle exhaustivo previsto en `docs/entregas.md` (aspiracional: ese archivo
todavía no existe; ver §8).

---

## 7. Convenciones de trabajo

- **Git:** commits incrementales y frecuentes, con participación visible de
  **todos** los integrantes. Nada de un único commit final.
- **Jira / Trello / Azure DevOps:** tablero al día **en todo momento**, no la
  víspera. Acceso de lectura al docente desde la primera entrega; puede pedirlo
  sin aviso.
- **Documentación = subproducto del trabajo.** Los `.md` de `docs/` se escriben a
  medida que se decide, y en cada entrega se exportan a PDF. Es el 15% de la
  rúbrica, obtenido sin trabajo extra.
- **Declarar uso de IA** en cada entrega obligatoria: qué se usó y para qué. Cada
  integrante debe poder defender cualquier parte igual.
- **App web:** responsive obligatorio.
- **App móvil:** adaptarse a distintos tamaños de pantalla y a la orientación.
- Prototipo navegable en Figma para el 19/10.

---

## 8. Estructura del repo

```
passly/
├── CLAUDE.md              ← este archivo
├── CLAUDE.local.md        ← rutas y entorno personales (gitignored)
├── README.md              ← qué es Passly y cómo levantarlo
├── docker-compose.yml     ← Postgres local de desarrollo (Artemis se suma en la Obligatoria 2)
├── db/init/               ← scripts de inicialización del contenedor (CREATE SCHEMA por componente)
├── .env.example           ← plantilla de variables de entorno, sin secretos
├── docs/
│   ├── ddl-eventos.sql    ← DDL de referencia del esquema eventos (Hibernate lo genera)
│   ├── ddl-productoras.sql ← ídem esquema productoras (incl. por qué miembro.usuario_id no lleva FK)
│   └── http/              ← peticiones .http (REST Client) + README:
│                            salud, cartelera con filtro, flujo feliz (incl. republicar → 409),
│                            errores (401/403/404/409), productoras, y **aislamiento entre
│                            productoras** — el guion central de la demo
├── backend/               ← Spring Boot, un paquete por componente
│   ├── pom.xml
│   ├── mvnw, mvnw.cmd, .mvn/  ← Maven wrapper
│   └── src/
│       ├── main/java/com/passly/     ← código (§4.3)
│       ├── main/resources/application.yml
│       └── test/java/com/passly/     ← EstructuraDeModulosTest, PasslyApplicationTests
└── (aspiracional — todavía no existen):
    ├── docs/arquitectura.md, docs/componentes.md, docs/patrones.md,
    │   docs/integraciones.md, docs/entregas.md, docs/adr/  ← se escriben a medida que se decide
    ├── web/               ← app web (comprador + organizador)
    └── app-validador/     ← app móvil del validador
```

> `CLAUDE.local.md` es un archivo personal opcional (gitignored); puede no existir
> en un clon recién hecho. Los `.md` de `docs/` listados como aspiracionales todavía
> no están escritos: hoy `docs/` contiene solo `ddl-eventos.sql` y `http/`.

---

## 9. Estado actual

**Entrega del 31/08 lista:** informe con arquitectura, 8 componentes con
interfaces, stack justificado y diagramas.

**`ServicioDeEventos` implementado y verificado end-to-end** (31/08): las tres capas
separadas, esquema `eventos` propio, Spring Modulith verificando fronteras y generando
documentación desde el código, datos de demo, y el flujo completo probado con curl —
incluido el 409 al intentar republicar un evento. Corre contra Postgres local en Docker; el
paso a Supabase es solo cambiar variables de entorno (§4.8).

**`ServicioDeUsuarios` implementado** (PAS-5): las tres capas separadas, esquema `usuarios`
propio, los roles del dominio (`COMPRADOR` / `ORGANIZADOR` / `VALIDADOR` / `ADMIN`) como parte
del contrato, y la credencial guardada **hasheada con BCrypt**. El `PasswordEncoder` es un
`@Bean` propio del componente (`spring-security-crypto` — solo la librería de hashing, sin
filter chain ni `@PreAuthorize`): **PAS-6 reutiliza ese mismo encoder para verificar el login**,
y el hash nunca sale del componente (el `UsuarioDTO` no lo incluye). El test de fronteras de
Modulith sigue en verde con el segundo módulo.

**`ServicioDeProductoras` implementado y Passly pasó a ser multi-productora** (PAS-13): las
tres capas separadas, esquema `productoras` propio con el padrón de miembros, y el evento
con dueño. Verificado end-to-end: la cartelera muestra fiestas de dos productoras distintas
y se puede filtrar por organizador; una productora recibe **403** al intentar publicar el
evento de otra, un `VALIDADOR` de la productora también (pertenecer ≠ poder gestionar), sin
identidad es **401**, y un `COMPRADOR` recibe **409** al intentar crear una productora.

Lo relevante para la defensa no es el CRUD sino el episodio de §4.4: al aparecer la
dependencia `eventos → productoras`, **el build falló** hasta declararla, y después falló el
test que afirmaba que Eventos era la raíz. Como efecto colateral, los datos de demo se
mudaron a `com.passly.demo`: sembrar un evento de Aurora exige actuar como alguien de
Aurora, y un seeder dentro de Eventos habría necesitado justamente la dependencia a Usuarios
que el diseño existe para evitar. Los datos de demo son un cliente del sistema, como la app
web, y usan solo contratos públicos.

**Seguridad implementada** (PAS-6): módulo **`seguridad/`** con Spring Security — **HTTP Basic**,
sesión **STATELESS**, autorización por rol declarativa con `@PreAuthorize` **en los controllers**
sobre 2 operaciones sensibles (Eventos: solo `ORGANIZADOR` crea/publica; Usuarios: el alta
pública solo crea `COMPRADOR`, el resto lo da de alta un `ADMIN`) más el bonus (listar usuarios
solo `ADMIN`). Autentica contra `usuarios` por el contrato `usuarios :: autenticacion`
(`CredencialDTO`) y **reutiliza el `PasswordEncoder`** del componente, sin crear un segundo bean.
Un test de integración (`AutorizacionPorRolTest`) fija que un `COMPRADOR` autenticado no puede
crear eventos. El test de fronteras de Modulith sigue verde con el cuarto módulo. Detalle en §4.11.

Integrar Seguridad con Productoras (ambas ramas partían del mismo `main`, previo a PAS-13) exigió
resolver algo más que el conflicto de texto en `EventoController`: el `@PreAuthorize` de PAS-6
estaba escrito sobre la firma vieja de `crearEvento`/`publicarEvento`, sin el `idUsuarioActuante`
que PAS-13 les agregó para verificar membresía. Se aprovechó el momento para migrar Eventos del
header temporal `X-Usuario-Id` a la identidad real de Spring Security — la migración que el propio
`CLAUDE.md` daba por "una línea por endpoint". No lo fue del todo: el `UserDetails` de PAS-6 solo
llevaba email y rol, sin el id numérico que Eventos necesita, y resolverlo llamando a
`UsuarioService` desde Eventos habría violado la dependencia declarada en su `package-info`
(`eventos → productoras`, nada de `usuarios`). La solución fue hacer que
`DetalleDeUsuarioParaAutenticacion` arme el `UserDetails` con el **id** como `username` en vez del
email: el login se sigue haciendo por email, pero `Authentication#getName()` después de
autenticar ya es el id, así que Eventos lo lee con un tipo de Spring Security, sin tocar Usuarios
(detalle completo en §4.11).

**Productoras migró a `Authentication`** (preparación de PAS-8): mismo patrón que Eventos,
`Authentication#getName()` en vez de `X-Usuario-Id`. No cambió ningún DTO ni ninguna firma de
`ProductoraService` — la identidad siempre viajó como parámetro aparte del *request body*. Se
hizo antes de `ServicioDeVentas` porque el propio CLAUDE.md ya lo pedía en §9, y porque un
componente nuevo que depende de identidad no debería construirse sobre un header falsificable
que está a un commit de desaparecer. El handler de `MissingRequestHeaderException` en
`ManejadorDeErroresDeProductoras` quedó sin uso y se eliminó.

**`ServicioDeVentas` implementado — el stateful y la transacción declarativa que cerraban
el checklist §6** (PAS-8): el componente con más dependencias salientes del sistema, y el
más complejo hasta ahora. Tres piezas nuevas de diseño, ninguna copiada mecánicamente de la
plantilla de `eventos`:

- **El carrito** (`CarritoDeCompra`, `@Component @SessionScope`) es el stateful. No reserva
  cupo — trade-off declarado, sobreventa optimista posible entre carritos simultáneos — y
  guarda quién es su dueño para blindarse del riesgo de sesión que trae `STATELESS`. Detalle
  completo, incluida la rotación de `JSESSIONID` en cada request que hay que saber explicar
  en el oral, en §4.7.
- **La transacción declarativa** vive en `ConfirmacionDeCompra`, un bean *aparte* de
  `VentaServiceImpl` — no por estilo, sino porque el cobro tiene que quedar afuera de la
  transacción (§2) y la auto-invocación rompe el proxy de `@Transactional` si ambas cosas
  compartieran clase. `EventoService.descontarCupo` se une a esa misma transacción por
  propagación `REQUIRED`: un solo commit para dos esquemas, el payoff concreto del ADR
  "monolito modular" (§4.2). Detalle en §4.11.
- **`PasarelaDePago` / `PasarelaDePagoSimulada`** es el tercer patrón de diseño del checklist
  (Port/Adapter, además de Facade y DAO), y resuelve sin bloquear el hecho de que
  `ServicioDePagos` (PAS-7) todavía no existe: el día que exista, se escribe un segundo
  adapter y `VentaServiceImpl` no se toca.

Se agregó también el contrato `EventoService.descontarCupo` (en lote, ordenado por id para
reducir la ventana de deadlock entre compras concurrentes) — el primer método de *escritura*
que `eventos` expone a otro componente, y el que pone a trabajar el `@Version` de
`TipoEntrada` que se había dejado preparado desde antes de que `ventas` existiera.

**Un bug real, encontrado probando en vivo y no en un test:** `consultarOrden` y
`listarOrdenesDeComprador` daban 500 (`LazyInitializationException` sobre `Orden.items`)
hasta agregarles `@Transactional(readOnly = true)` a nivel de método — sin contradecir el
"`VentaServiceImpl` sin `@Transactional` de clase", porque esa anotación puntual no envuelve
ningún cobro, solo una lectura. Quedó cubierto por `ConfirmarCompraTest` para que no vuelva.

Verificado end-to-end contra la app real (perfil demo): el carrito crece y funde cantidades
del mismo tipo de entrada, confirmar descuenta cupo real y cobra, y el rollback central — un
carrito de dos líneas donde la segunda excede el cupo — revierte también la primera línea
(que ya había hecho `flush`) y compensa el cobro, dejando el carrito intacto para reintentar.

**Próximo paso inmediato:** **`ServicioDeTickets`** — firma criptográfica del QR. Es el seam
que `ConfirmacionDeCompra` ya dejó preparado (hoy no emite ningún ticket; la emisión está
fuera del alcance de PAS-8) y lo que necesita `ServicioDeAccesos` para validar en la puerta.

**Orden de implementación sugerido** (sale del grafo de dependencias):

1. ~~`ServicioDeEventos` — primer componente implementado~~ ✅ hecho
2. ~~`ServicioDeUsuarios` — roles y credenciales, raíz del grafo~~ ✅ hecho
3. ~~`ServicioDeProductoras` — múltiples organizadores, eventos con dueño~~ ✅ hecho
4. ~~**Seguridad (PAS-6)** — HTTP Basic + `@PreAuthorize` por rol; módulo `seguridad/`~~ ✅ hecho
5. ~~`ServicioDeVentas` — el stateful, con callbacks de ciclo de vida~~ ✅ hecho
6. `ServicioDeTickets` — firma criptográfica del QR
7. El resto, según lo que pida cada entrega

---

## 10. Dudas abiertas

- **Framework de la app web y stack de la app móvil:** decididos como React /
  React Native, falta confirmarlo con el equipo.
- **Fecha de la Entrega Final:** la tabla del TP dice **21/12**; el detalle de esa
  misma entrega dice **30/11**. Confirmar con la cátedra por Teams.
- **Nota mínima de la final:** la tabla dice **mín. 4**; el detalle dice
  **mín. 8 → aprobación directa**. Confirmar.
- **¿Separar `ServicioDeInventario` de `ServicioDeVentas`?** Sigue sin resolverse, y
  PAS-8 se implementó sin esa separación: Ventas es a la vez Facade y titular del
  carrito, dos responsabilidades en un componente. El cupo en sí no está duplicado —
  vive solo en `eventos.tipo_entrada`, con `EventoService.descontarCupo` como única
  puerta de escritura —, así que lo que quedaría por extraer es específicamente el
  carrito stateful, no el stock. **El argumento de "suma un componente más" ya no
  aplica:** Productoras llevó el total a 9, muy por encima del mínimo de 6. La
  decisión se juega solo en si vale la pena el diseño más limpio.
- Confirmar que la comisión maneja las mismas fechas de checkpoints.

### Deuda técnica declarada

- **`ddl-auto: update` → migrar a Flyway (o `validate`) antes de la Obligatoria 2.**
  Hoy Hibernate crea y evoluciona las tablas al arrancar (`ddl-auto: update` en
  `application.yml`). Sirve para el desarrollo diario, pero no versiona el esquema
  ni sabe borrar columnas. El plan —declarado hasta ahora solo en comentarios de
  `backend/src/main/resources/application.yml` y `docs/ddl-eventos.sql`— es pasar a
  Flyway, o exportar el DDL y usar `ddl-auto: validate`, antes de la Obligatoria 2.
  `docs/ddl-eventos.sql` es el punto de partida para esa migración.
- **Compensación del cobro, best-effort (PAS-8).** Si `ConfirmacionDeCompra.confirmar`
  falla después de que la pasarela aprobó el cobro, `VentaServiceImpl` llama a
  `PasarelaDePago.revertir` y relanza. Si *esa* llamada también fallara, el cobro
  queda huérfano y hoy solo hay un log en `ERROR` — no hay outbox ni reintento. Es
  el mismo problema de doble escritura que la cola `orden.pagada` de la Obligatoria
  2 va a resolver correctamente; acá queda declarado como deuda, no escondido.
- **Sin tabla `ventas.pago`, a propósito.** `ventas.orden` guarda una *referencia*
  al cobro (`comprobante_cobro`, `cobrada_en`), no su registro. Crear una tabla de
  pagos hoy, antes de que `ServicioDePagos` (PAS-7) exista como módulo propio, sería
  crear algo que después habría que borrar o duplicar en el esquema de ese módulo.