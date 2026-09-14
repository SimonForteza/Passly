# Guia de justificacion de tecnologias y diseno de Passly

Esta guia conecta los temas de Desarrollo de Aplicaciones II con decisiones concretas del proyecto. Esta pensada para la exposicion oral: no alcanza con decir que tecnologia se uso; hay que explicar que problema resuelve, que alternativa se evaluo, que costo se acepto y donde se ve la decision funcionando.

## 1. Formula para responder en la defensa

Para cualquier pregunta, responder en este orden:

1. **Necesidad:** que problema del dominio o de la consigna habia que resolver.
2. **Decision:** que tecnologia o diseno se eligio.
3. **Alternativas:** que otras opciones eran razonables.
4. **Trade-off:** que se gano y que costo o limitacion se acepto.
5. **Evidencia:** archivo, prueba o recorrido ejecutable que demuestra la decision.
6. **Estado real:** distinguir implementado, deuda tecnica y trabajo futuro.

Ejemplo corto:

> Necesitabamos fronteras entre componentes, pero tambien una transaccion local que descuente cupo y registre la orden. Elegimos un monolito modular con Spring Modulith. Un monolito tradicional era mas simple pero no hacia cumplir las fronteras; microservicios daban despliegue independiente, pero obligaban a usar sagas y agregaban fallos de red. Aceptamos desplegar todo junto. La evidencia es `EstructuraDeModulosTest`, que hace fallar el build ante dependencias no permitidas.

## 2. Estado que debe declararse antes de justificar

### Implementado

- Seis modulos backend: Usuarios, Productoras, Eventos, Seguridad, Ventas y Pagos.
- Contratos publicos, DTO y encapsulamiento de implementaciones bajo `internal`.
- Arquitectura de presentacion, negocio y datos.
- DAO/Repository con Spring Data JPA.
- Facade en `VentaService`.
- Adapter REST en `PagoService`.
- Componente stateful: carrito de Ventas con `@SessionScope`.
- Componentes stateless: Usuarios, Productoras, Eventos, Seguridad y Pagos.
- Autenticacion HTTP Basic y autorizacion declarativa con Spring Security.
- Transaccion local que descuenta cupo y persiste la orden.
- Frontend React para identidad, productoras, cartelera y backoffice de eventos.
- Verificacion de fronteras con Spring Modulith y GitHub Actions.

### Pendiente o planificado

- Ventas todavia usa `PasarelaDePagoSimulada`; falta conectarlo con el `PagoService` real.
- Tickets y Accesos todavia no estan implementados.
- Cola JMS para Facturacion y topico para Notificaciones/Accesos estan disenados, no integrados.
- Adapter SOAP de Facturacion hacia AFIP esta planificado, no implementado.
- Las pantallas web de carrito y ordenes siguen pendientes.

Esta distincion fortalece la defensa: demuestra que el equipo conoce el limite entre una decision arquitectonica y su implementacion efectiva.

## 3. Unidad I: Programacion orientada a componentes y ecosistema

### 3.1 Por que Passly esta dividido en componentes

El dominio tiene capacidades con responsabilidades diferentes: una cosa es identificar usuarios, otra administrar productoras, otra publicar eventos y otra vender entradas. Separarlas aumenta la cohesion: cada modulo cambia por motivos relacionados con su propia capacidad.

Las dependencias se hacen mediante interfaces como `UsuarioService`, `ProductoraService`, `EventoService`, `VentaService` y `PagoService`. Las entidades JPA, repositorios, controllers e implementaciones quedan bajo `internal`. Los componentes no comparten entidades ni consultan tablas ajenas.

**Relacion con POC:** el contrato es estable y la implementacion queda encapsulada. En Passly los modulos forman un unico JAR, por lo que no se afirma que cada uno tenga despliegue binario independiente. La autonomia buscada es logica y verificable dentro de un monolito modular.

**Evidencia:**

- `backend/src/main/java/com/passly/*/package-info.java`
- Interfaces publicas en la raiz de cada modulo.
- DTO publicados como `@NamedInterface("dto")`.
- Implementaciones bajo `internal`.

### 3.2 Por que Spring Boot y no Jakarta EE puro

Spring Boot ofrece el contenedor que necesita la materia: inversion de control, inyeccion de dependencias, scopes, proxies transaccionales, seguridad declarativa, persistencia y servidor web embebido. Permite empaquetar Passly como un JAR ejecutable y reduce configuracion operativa para un equipo pequeno.

Jakarta EE era una alternativa valida y mas portable entre servidores como WildFly o Payara. No se eligio porque el equipo no necesitaba cambiar de proveedor de contenedor ni desplegar EJB en un servidor externo. Se priorizo velocidad de desarrollo y una demo reproducible.

El costo es mayor dependencia del ecosistema Spring. Se mitiga usando estandares Jakarta donde corresponde: JPA, Bean Validation y anotaciones de ciclo de vida.

### 3.3 Que administra el contenedor en Passly

- Crea e inyecta servicios, controllers y repositorios; el codigo cliente no hace `new` de las implementaciones.
- Construye proxies para aplicar `@Transactional` y `@PreAuthorize`.
- Crea una instancia de `CarritoDeCompra` por sesion mediante `@SessionScope`.
- Ejecuta `@PostConstruct` y `@PreDestroy` del carrito.
- Administra conexiones JPA/Hibernate y threads HTTP de Tomcat.
- Construye el `SecurityFilterChain` y autentica cada request.

Frase util:

> La diferencia con POO no es que dejamos de usar objetos; es que el ciclo de vida y los servicios transversales de esos objetos los administra el contenedor de forma declarativa.

### 3.4 Spring Boot frente a .NET

.NET tambien ofrecia DI, ORM, seguridad y despliegue web. La eleccion de Java/Spring se justifica por alineacion con la catedra, experiencia del equipo, disponibilidad de Spring Modulith y continuidad con JPA/JMS/SOAP. No se sostiene que .NET sea incapaz; se eligio el stack que permitia demostrar mejor los conceptos exigidos con menor riesgo de entrega.

## 4. Unidad II: componentes y arquitectura en capas

### 4.1 Por que se usan tres capas dentro de cada componente

Cada modulo aplica el flujo:

`Controller -> Service -> Repository`

- **Presentacion:** interpreta HTTP, valida la forma de la solicitud y traduce errores a estados HTTP.
- **Negocio:** aplica reglas, autorizacion de alcance, coordinacion y limites transaccionales.
- **Datos:** persiste entidades mediante repositories JPA.

La alternativa descartada fue hacer controllers que operaran directamente con repositories. Era mas corta, pero mezclaba protocolo, reglas y persistencia; dificultaba probar, reutilizar y cambiar cada parte.

La regla no implica crear capas horizontales globales. Passly usa rebanadas verticales: cada componente contiene sus propias tres capas. Asi una funcionalidad se puede entender y modificar sin recorrer carpetas compartidas por todo el sistema.

### 4.2 Por que las entidades no salen del componente

Una entidad JPA refleja decisiones internas de persistencia. Si Eventos entregara su entidad a Ventas, un cambio de tabla afectaria consumidores y permitiria modificaciones fuera de las reglas del propietario. Por eso se cruzan fronteras con DTO inmutables y contratos de negocio.

Trade-off: existe codigo de mapeo adicional. Se acepta porque reduce acoplamiento y evita que la base de datos se convierta en la interfaz comun de todos los modulos.

### 4.3 Stateless: por que la mayoria de los componentes no conserva conversacion

Usuarios, Productoras, Eventos, Seguridad y Pagos atienden cada operacion usando los datos de la solicitud y el estado persistido. Guardar entidades en PostgreSQL no vuelve stateful al componente: no existe una conversacion privada en memoria asociada a un cliente.

Ventaja: las instancias se pueden compartir entre requests y escalar con mayor facilidad. En Spring, los servicios son beans singleton por defecto, pero deben escribirse sin estado mutable por usuario.

### 4.4 Stateful: por que el carrito si conserva estado

La seleccion de entradas crece durante una conversacion de compra y pertenece a un comprador. `CarritoDeCompra` usa `@SessionScope`, tiene una instancia por sesion, registra su ciclo de vida y vence cinco minutos despues de crearse.

La alternativa de produccion seria persistir el carrito en PostgreSQL o Redis. Eso permitiria escalar horizontalmente y sobrevivir reinicios, pero ocultaria el ejemplo de estado conversacional administrado por el contenedor que pide la materia. Para el alcance academico se eligio memoria de sesion y se declaro su limitacion.

Importante: la autenticacion sigue siendo stateless. La cookie identifica el carrito, no una sesion de login; las credenciales Basic se presentan en cada request.

### 4.5 Por que no se uso un Singleton de negocio como ejemplo

Los servicios Spring son singleton por defecto, pero no se usa esa circunstancia como patron de dominio ni como almacenamiento global. Un carrito singleton mezclaria compras de usuarios distintos y generaria problemas de concurrencia. El scope elegido surge de la semantica del caso de uso, no de conveniencia tecnica.

## 5. Unidad III: patrones y calidad

### 5.1 Cohesion y acoplamiento

**Cohesion:** cada modulo agrupa una capacidad de negocio y sus datos. Pagos conoce cobros, no eventos; Usuarios conoce identidades, no ordenes.

**Bajo acoplamiento:** Ventas consulta Eventos y Usuarios mediante contratos. Spring Modulith declara las aristas permitidas y hace fallar el build si un modulo importa internos ajenos o crea ciclos.

**Reutilizacion:** el objetivo principal no fue reutilizar el JAR de cada modulo en otros productos, sino reutilizar contratos y aislar cambios. Es mejor declarar este alcance que prometer reutilizacion binaria no demostrada.

### 5.2 DAO / Repository

Los repositories de Spring Data extienden `JpaRepository` y ocultan consultas, `EntityManager`, conexiones y detalles de Hibernate. La capa de negocio expresa operaciones de dominio sin escribir SQL.

Alternativa: JDBC o SQL directo dentro del service. Daria mas control, pero aumentaria codigo repetitivo y acoplaria reglas con persistencia. JPA es apropiado porque el dominio actual es relacional y las transacciones son centrales.

**Evidencia:** `UsuarioRepository`, `ProductoraRepository`, `EventoRepository`, `OrdenRepository` y `PagoRepository`.

### 5.3 Facade

`VentaService` ofrece al controller una interfaz unificada para agregar items, consultar/vaciar carrito, confirmar una compra y consultar ordenes. El cliente no coordina por su cuenta Usuarios, Eventos, cobro y persistencia.

La Facade reduce complejidad para el consumidor, pero puede convertirse en un servicio demasiado grande. Se controla delegando la parte transaccional en `ConfirmacionDeCompra` y manteniendo responsabilidades ajenas en sus componentes propietarios.

### 5.4 Adapter

`PagoService` define el modelo que Passly necesita y `PagoServiceImpl` traduce hacia la API REST de la pasarela. El proveedor externo no impone sus DTO al resto de la aplicacion.

Ventaja: se puede cambiar el proveedor o usar un mock conservando el contrato interno. Costo: hay clases de traduccion y manejo de errores adicionales.

No confundir dos situaciones:

- Pagos ya implementa el Adapter REST externo.
- Ventas todavia usa su `PasarelaDePagoSimulada`; falta un Adapter que conecte ese puerto interno con `PagoService`.

### 5.5 Por que no se presenta Factory como patron aplicado

El contenedor crea beans y Spring Data genera repositories, pero eso no alcanza para afirmar que el equipo implemento intencionalmente Factory como patron de negocio. Para la entrega se defienden DAO, Facade y Adapter, que tienen problema, estructura y evidencia explicitos.

## 6. Unidad IV: EAI y SOA

### 6.1 Que tipo de integracion usa cada relacion

| Relacion | Tipo de integracion | Motivo |
|---|---|---|
| Frontend -> backend | Procedural sincrona REST/JSON | La interfaz necesita respuesta inmediata |
| Pagos -> pasarela | Procedural sincrona REST/JSON | La compra necesita saber si el cobro fue autorizado |
| Ventas -> Facturacion | Proceso asincronico por cola, planificado | Facturar no debe bloquear ni revertir la venta |
| Tickets -> Notificaciones/Accesos | Publicacion/suscripcion, planificado | Varios consumidores reaccionan al mismo evento |
| Facturacion -> AFIP | SOAP, planificado | Simula una integracion corporativa de contrato estricto |

### 6.2 Por que no integrar por base de datos compartida

Aunque los modulos usan una misma instancia PostgreSQL, no comparten tablas. Cada uno tiene su esquema y propietario. Ventas no hace joins con Eventos: llama a `EventoService`.

Compartir tablas seria simple y rapido, pero convertiria el esquema en un contrato implicito, permitiria saltar reglas de negocio y aumentaria el acoplamiento. El esquema por componente mantiene transacciones locales sin renunciar a la separacion logica.

### 6.3 Por que no archivos, portales o un ESB

- Archivos batch no sirven para disponibilidad o cobro en tiempo real.
- Un portal integra vistas, pero no resuelve los procesos y contratos del backend.
- Un ESB aportaria ruteo y transformacion, pero es complejidad excesiva para pocas integraciones y un solo equipo.

El diseno actual combina llamadas directas para respuestas inmediatas y mensajeria para efectos posteriores. Si el numero de sistemas creciera, podria revisarse la necesidad de un hub o bus.

### 6.4 SOA y monolito modular no son sinonimos ni enemigos

Passly aplica principios compatibles con SOA —capacidades de negocio, contratos explicitos y bajo acoplamiento— sin desplegar cada capacidad como servicio remoto. SOA describe un estilo de servicios; microservicios agrega decisiones de granularidad y despliegue. Para el alcance actual, las llamadas locales reducen costo y mantienen transacciones ACID.

## 7. Unidad V: integracion asincronica

### 7.1 Cola punto a punto para Facturacion

Una orden pagada debe generar una factura una sola vez. Por eso el diseno usa una cola `orden.pagada`: un mensaje es atendido por un consumidor de Facturacion. Si AFIP esta caido, el broker conserva el mensaje y permite reintentar sin bloquear al comprador.

Riesgos a explicar cuando se implemente:

- entrega al menos una vez y necesidad de consumidores idempotentes;
- reintentos con backoff;
- dead-letter queue para errores permanentes;
- correlacion y observabilidad del mensaje.

### 7.2 Topico para ticket emitido

La emision de un ticket interesa a mas de un componente: Notificaciones envia el ticket y Accesos prepara su validacion. Un topico permite que ambos reciban el mismo evento sin que Tickets conozca cada proceso interno.

No se usa una cola unica porque alli un solo consumidor recibiria cada mensaje; se perderia uno de los dos efectos.

### 7.3 Por que ActiveMQ Artemis

Artemis implementa JMS de forma nativa y se alinea directamente con la unidad de mensajeria. RabbitMQ era viable como broker AMQP, pero requeria explicar una capa distinta al estandar evaluado. Ejecutarlo en Docker hace visible la infraestructura y evita esconder el broker dentro del proceso.

**Estado honesto:** esta justificacion corresponde al diseno previsto; JMS y Artemis todavia no estan integrados en el codigo actual.

## 8. Unidad VI: servicios web

### 8.1 Por que REST en la API de Passly

Los clientes trabajan con recursos como usuarios, productoras, eventos, carritos y ordenes. HTTP ofrece verbos, estados y JSON compatibles con navegador y app movil. REST reduce el peso del contrato y facilita inspeccionar la demo con navegador, `curl` o archivos `.http`.

Decisiones concretas:

- GET para consultar sin modificar.
- POST para altas, publicacion y confirmacion de acciones.
- 401 cuando falta identidad y 403 cuando existe identidad sin permiso.
- 404 para una orden ajena, para no revelar que existe.
- `ProblemDetail` para errores HTTP consistentes.

### 8.2 Por que HTTP Basic en esta entrega

Basic permite demostrar autenticacion real, BCrypt y autorizacion declarativa sin introducir emision, renovacion y revocacion de tokens. Las credenciales se envian en cada request y el backend no mantiene una sesion de autenticacion.

Costo: debe usarse exclusivamente sobre HTTPS en un entorno real y no ofrece logout/revocacion equivalente a tokens. El frontend guarda las credenciales en `sessionStorage`, una simplificacion aceptable para la demo pero no la opcion recomendada para produccion. Una evolucion posible es OAuth2/OIDC con tokens de corta duracion.

### 8.3 Por que SOAP para AFIP

SOAP con WSDL representa una integracion corporativa o legada con contrato estricto, tipos XML y operaciones formalmente definidas. En Facturacion permite demostrar un protocolo diferente de REST y aislarlo detras de un Adapter.

REST seria mas liviano, pero no cumple el objetivo academico de interoperar con un servicio SOAP. La llamada debe ocurrir fuera de la transaccion de venta y detras de mensajeria, porque un sistema externo lento no es rollbackeable.

**Estado honesto:** Facturacion y el cliente SOAP estan planificados, no implementados.

## 9. Decisiones transversales que suelen preguntar

### 9.1 Monolito modular frente a microservicios

Se eligio un unico despliegue con fronteras internas verificadas. Microservicios permitirian escalar y desplegar componentes por separado, pero agregarian red, observabilidad distribuida, contratos remotos y consistencia eventual. Para un equipo pequeno y una transaccion que modifica Eventos y Ventas, ese costo no estaba justificado.

El costo del monolito modular es desplegar todo junto y no escalar cada modulo de forma independiente. Si mediciones futuras muestran cargas o ciclos de despliegue muy distintos, los contratos actuales facilitan extraer un componente.

### 9.2 PostgreSQL y esquema por componente

PostgreSQL ofrece transacciones ACID, restricciones y un modelo relacional adecuado para identidades, membresias, cupos, ordenes y pagos. Una sola instancia simplifica la demo; un esquema por componente hace visible la propiedad de datos.

Base fisica por componente daria mayor aislamiento, pero impediria una transaccion local sencilla entre Eventos y Ventas. Un esquema unico compartido simplificaria consultas, pero destruiria las fronteras. La decision intermedia conserva separacion logica y operacion simple.

### 9.3 Supabase

Supabase se usa como PostgreSQL gestionado, no como reemplazo del backend. El frontend nunca usa PostgREST directamente porque eso saltearia negocio, seguridad y contratos. Tampoco se delega la autorizacion de negocio en Supabase Auth: vive en Spring Security.

### 9.4 React, Vite y TypeScript

React permite construir una interfaz por componentes y compartir estructura entre recorridos de comprador y organizador. Vite reduce configuracion y acelera el ciclo de desarrollo. TypeScript agrega chequeo estatico sobre DTO y respuestas del backend.

La alternativa de renderizar vistas desde Spring reducia la cantidad de proyectos, pero acoplaba presentacion y backend y no preparaba una experiencia reutilizable para clientes distintos. Angular aportaba una estructura mas prescriptiva, pero tenia mayor costo inicial para el alcance y experiencia del equipo.

### 9.5 Maven, Docker y GitHub Actions

- Maven fija dependencias, version de Java y un build reproducible.
- Docker Compose estandariza PostgreSQL y, mas adelante, el broker; reduce diferencias entre maquinas.
- GitHub Actions ejecuta la prueba de fronteras en cada PR y evita que la modularidad dependa solo de revision humana.

Docker no elimina todas las diferencias: volumenes, puertos y recursos siguen requiriendo cuidado. El workflow actual verifica Modulith sin base; la suite integral contra PostgreSQL sigue siendo una evidencia separada.

## 10. Seguridad y transacciones como servicios del contenedor

### 10.1 Seguridad declarativa

El `SecurityFilterChain` define reglas generales y `@PreAuthorize` expresa permisos cerca de cada operacion sensible. El rol global es un filtro grueso; la pertenencia a una productora es una regla fina de negocio.

Se colocaron permisos por rol en controllers para no acoplar los servicios de dominio a Spring Security y permitir que los seeders llamen contratos durante el arranque. Las comprobaciones de alcance siguen en negocio porque deben cumplirse sin importar el adaptador de entrada.

### 10.2 Por que el cobro esta fuera de `@Transactional`

Una transaccion de base no puede deshacer una operacion ya aceptada por una pasarela externa. Mantenerla abierta durante la llamada HTTP tambien retendria conexiones o locks.

El flujo actual es:

1. Ventas intenta cobrar fuera de la transaccion local.
2. `ConfirmacionDeCompra`, en otro bean, abre `@Transactional`.
3. Eventos descuenta cupo y Ventas persiste la orden en la misma base fisica.
4. Si falla la parte local, se intenta revertir el cobro como compensacion de mejor esfuerzo.

La separacion en dos beans es necesaria porque Spring aplica `@Transactional` mediante un proxy; una llamada a `this.confirmar()` no atravesaria el proxy.

### 10.3 Que garantiza y que no garantiza la transaccion

Garantiza atomicidad local entre el descuento de cupo y la orden. No garantiza atomicidad distribuida con la pasarela, por eso existe compensacion. Tampoco incluye Tickets porque ese componente aun no existe.

## 11. Preguntas probables y respuestas breves

### ¿Por que dicen que son componentes si se despliega un solo JAR?

Porque cada capacidad tiene contrato publico, implementacion y datos encapsulados, y sus dependencias se verifican automaticamente. El despliegue independiente es una propiedad posible de algunos modelos de componentes, no un requisito que el equipo afirme haber implementado.

### ¿Que impide que Eventos use el repository de Usuarios?

La convencion `internal` expresa la intencion y Spring Modulith verifica la frontera. `ApplicationModules.verify()` hace fallar el build ante imports no permitidos o ciclos.

### ¿Por que Seguridad es stateless si existe `JSESSIONID`?

Porque las credenciales Basic se validan en cada request. La sesion se usa solo para el estado conversacional del carrito; no representa una sesion de autenticacion.

### ¿Por que el carrito no se guarda en la base?

Para demostrar explicitamente un componente stateful administrado por el contenedor. Se acepta que no escale horizontalmente ni sobreviva reinicios; para produccion se migraria a almacenamiento compartido.

### ¿Facade no es simplemente un Service?

No todo service es Facade. `VentaService` lo es porque presenta una interfaz simple y oculta la coordinacion de varios colaboradores y pasos del subsistema de compra.

### ¿Por que Pagos es Adapter?

Porque traduce entre el contrato propio `PagoService` y el contrato HTTP de un proveedor externo. El resto de Passly no conoce sus DTO ni errores.

### ¿Por que no hicieron todo asincronico?

Disponibilidad y autorizacion de cobro necesitan respuesta inmediata. Facturacion y notificacion pueden ocurrir despues y se benefician del desacoplamiento temporal.

### ¿Por que no usar microservicios desde el principio?

No habia necesidad comprobada de despliegue o escalado independiente. Con el equipo y alcance actuales agregaban mas fallos y una saga sin aportar valor proporcional.

### ¿Que pasa si dos compradores toman el ultimo cupo?

Agregar al carrito no reserva. La disponibilidad real se valida y descuenta al confirmar dentro de la transaccion. Uno confirma; el otro recibe cupo insuficiente y conserva su carrito para corregir o reintentar.

### ¿Que pasa si se cobra y falla la orden?

La transaccion local revierte cupo y orden, y Ventas intenta compensar el cobro. Se reconoce que una compensacion externa puede fallar; un sistema productivo necesita reintentos, conciliacion y observabilidad.

### ¿Esta terminada la integracion de Pagos?

El componente Pagos y su Adapter REST estan implementados. La conexion desde Ventas sigue pendiente; Ventas usa hoy una pasarela simulada. No deben presentarse como una unica integracion terminada.

### ¿Por que la documentacion puede considerarse evidencia?

Por si sola no alcanza. Se sostiene con ADR versionados, pruebas de Modulith, historial incremental, CI y una demo en vivo. El informe explica decisiones; el codigo y la ejecucion las verifican.

## 12. Guion sugerido de exposicion

### Apertura - 1 minuto

1. Problema: plataforma multi-productora para publicar, vender y validar entradas.
2. Restricciones academicas: componentes, capas, estado, patrones, seguridad, transacciones e integraciones.
3. Decision principal: monolito modular con una base PostgreSQL y esquema por componente.

### Arquitectura - 3 minutos

1. Mostrar los seis modulos integrados y sus contratos.
2. Explicar `internal`, DTO y `allowedDependencies`.
3. Mostrar el grafo: Ventas depende de Eventos y Usuarios; Eventos de Productoras; Productoras de Usuarios; Pagos es raiz independiente.
4. Ejecutar o mostrar el resultado verde de `EstructuraDeModulosTest`.

### Conceptos de la materia - 4 minutos

1. Contenedor: IoC, scopes, proxies, seguridad y transacciones.
2. Capas: Controller, Service, Repository.
3. Stateful contra stateless usando el carrito.
4. Patrones: DAO, Facade y Adapter.
5. REST implementado; JMS y SOAP como evolucion declarada.

### Recorrido funcional - 3 minutos

1. Login e identidad.
2. Alta de productora y miembro.
3. Creacion y publicacion de evento.
4. Cartelera y disponibilidad.
5. Compra por API: cobro simulado, descuento de cupo y orden transaccional.

### Cierre - 1 minuto

1. Repetir que la arquitectura prioriza cohesion, bajo acoplamiento y demo reproducible.
2. Nombrar las deudas sin esconderlas: cableado Ventas-Pagos, Tickets, JMS, SOAP y frontend de compra.
3. Explicar como los contratos permiten incorporar esas piezas sin romper los modulos existentes.

## 13. Evidencias para tener abiertas durante la defensa

- `docs/adr/ADR-001-monolito-modular-vs-microservicios.md`
- `docs/adr/ADR-002-esquema-por-componente-vs-base-por-componente.md`
- `backend/src/test/java/com/passly/EstructuraDeModulosTest.java`
- `backend/src/main/java/com/passly/ventas/internal/negocio/CarritoDeCompra.java`
- `backend/src/main/java/com/passly/ventas/internal/negocio/ConfirmacionDeCompra.java`
- `backend/src/main/java/com/passly/pagos/internal/negocio/PagoServiceImpl.java`
- `backend/src/main/java/com/passly/seguridad/internal/ConfiguracionDeSeguridad.java`
- `docs/http/` para casos ejecutables.
- `output/pdf/informe-obligatoria-1.pdf` como documento de entrega.

## 14. Regla final para el equipo

No memorizar definiciones aisladas. Cada integrante debe poder conectar un concepto con una decision, una consecuencia y una evidencia ejecutable. Cuando algo este pendiente, decirlo y explicar como encaja en el diseno vale mas que presentarlo falsamente como terminado.
