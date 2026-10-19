# Passly

Plataforma de venta y validacion de entradas para eventos. Ver [CLAUDE.md](CLAUDE.md) para la
arquitectura completa, el stack y las decisiones de diseno.

## Qué tiene que hacer Passly (visión de producto)

Las productoras crean eventos y comparten su link; quien entra por ese link compra ahí mismo, y
quien entra sin link elige desde la cartelera de todas las productoras. El pago pasa por la
pasarela integrada, cada entrada sale con un QR firmado, y en la puerta un validador lo escanea y
lo marca como usado. El detalle de los cinco recorridos y qué falta para cada uno está en
CLAUDE.md §1 ("Visión de producto").

## Estado actual

Passly es **multi-productora**: un comprador elige entre fiestas de distintos organizadores y
cada productora publica y gestiona solo las suyas.

Nueve modulos backend implementados: `usuarios`, `productoras`, `eventos`, `seguridad`, `ventas`,
`pagos`, `tickets`, `accesos` y `facturacion` (esta ultima sin SOAP todavia). Spring Modulith verifica sus fronteras en el build y las dependencias estan
**declaradas** modulo por modulo. El grafo de negocio es aciclico: `ventas` depende directamente
de `eventos`, `usuarios`, `tickets` y `pagos`; `eventos -> productoras -> usuarios`; `pagos` y `tickets` no dependen de otro
modulo. Ventas cobra a traves de `PagoService` (PAS-29): el checkout manda la tarjeta, el cobro
pasa por el Adapter REST real hacia la pasarela (mockeada), y si la confirmacion falla despues de
cobrar, el cobro se revierte. La compra ya emite un ticket con QR firmado por cada unidad adquirida.

La identidad de quien opera la resuelve **Spring Security** (HTTP Basic, sesion STATELESS):
ningun endpoint depende de un header propio para saber quien esta operando. La autorizacion
por rol es declarativa con `@PreAuthorize` en los controllers (ver [CLAUDE.md](CLAUDE.md) 4.11).

La app web (`frontend/`) integra identidad, productoras, cartelera/backoffice, carrito, checkout,
ordenes y visualizacion de los QR emitidos. El login no usa
un endpoint de "login" (Basic no lo tiene): valida email + password contra
`GET /api/usuarios/me`.

## Documentación para usuarios y prototipo

- [Manual de usuario](docs/manual-de-usuario.md): cómo comprar, organizar eventos y validar entradas, con capturas.
- [Documentación funcional](docs/funcional.md): historias de usuario y su estado real.
- [Prototipo navegable en Figma](https://www.figma.com/proto/aQFl7PEjpuIVy9DjmbPJ3W/Passly?node-id=4-3&starting-point-node-id=4%3A3)
  (flujos de comprador, organizador y validador).
- PDFs de la entrega del Checkpoint 2: [docs/entrega-c2/](docs/entrega-c2/).

## Levantar el entorno

Requisitos: Java 21, Maven (o el wrapper `./mvnw` incluido), Docker.

```bash
# 1. Base de datos (Postgres local, descartable)
docker compose down -v && docker compose up -d

# 2. Backend, con datos de demo
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

El `down -v` recrea el volumen, y hace falta la primera vez despues de un cambio de esquema:
`db/init/` solo corre en la inicializacion del volumen, y con `ddl-auto: update` Hibernate no
agrega una columna `not null` a una tabla que ya tiene filas. Despues, `up -d` alcanza.

Sin el perfil `demo`, la base arranca vacia. Sin variables de entorno, el backend se conecta
al Postgres de `docker-compose.yml` (ver [.env.example](.env.example) para apuntar a Supabase).

## Levantar la app web

Requisito: Node 20+. Con el backend corriendo en `:8080` (paso anterior):

```bash
cd frontend
cp .env.example .env   # VITE_API_BASE_URL=http://localhost:8080 por default
npm install
npm run dev             # sirve en http://localhost:5173
```

El backend permite CORS desde `http://localhost:5173` por default
(`passly.web.origen` en `application.yml`, ver [CLAUDE.md](CLAUDE.md) 9) — si se corre la app
web en otro puerto u origen, hay que pisar esa property.

## Probar que anda

```bash
curl http://localhost:8080/actuator/health           # {"status":"UP"}
curl http://localhost:8080/api/productoras           # las dos productoras de demo
curl http://localhost:8080/api/eventos               # cartelera, cada evento con su organizador
curl "http://localhost:8080/api/eventos?productora=1" # solo las fiestas de Aurora
curl http://localhost:8080/actuator/modulith         # modelo de modulos detectado

# El corazon de la demo: Olga es de otra productora -> 403
curl -i -X POST http://localhost:8080/api/eventos/3/publicacion -u organizador2@passly.test:passly1234
# Omar es el dueno de Aurora -> 200
curl -i -X POST http://localhost:8080/api/eventos/3/publicacion -u organizador@passly.test:passly1234
```

Guion completo de demo como archivos `.http` listos para la extension REST Client de VS Code
en [docs/http/](docs/http/), incluido
[06-aislamiento.http](docs/http/06-aislamiento.http): una productora no toca las fiestas de
otra.

## Correr los tests

```bash
cd backend
./mvnw test
```

Incluye `EstructuraDeModulosTest`, que verifica las fronteras entre modulos
(`ApplicationModules.verify()`), afirma la forma del grafo de dependencias y genera la
documentacion de la arquitectura desde el codigo en `target/spring-modulith-docs/`.

Para ver que las fronteras estan realmente verificadas y no solo documentadas: comentar la
anotacion `@ApplicationModule` de cualquier `package-info.java` y volver a correr los tests.
Falla con `Module 'eventos' depends on ... Allowed targets: none`.

## Nota para Windows: certificados y timezone

- Si Maven falla resolviendo plugins con `PKIX path building failed`, es un antivirus haciendo
  inspeccion TLS (Norton, en este equipo) cuyo certificado raiz el JDK no reconoce por defecto
  aunque Windows si. Se soluciona importando ese certificado al truststore de Java.
- La JVM esta fijada a `-Duser.timezone=UTC` en `pom.xml` (ver el comentario ahi): en Windows,
  Java reporta el timezone del sistema como `America/Buenos_Aires`, un alias que la imagen
  oficial de Postgres no reconoce (solo tiene `America/Argentina/Buenos_Aires`), y la conexion
  JDBC fallaba con `FATAL: invalid value for parameter "TimeZone"`.

## Mensajeria (PAS-32)

`docker compose up -d` levanta Postgres y ActiveMQ Artemis. Consola: http://localhost:8161/console,
usuario y clave de desarrollo `passly` (externalizables en `.env`). La cola durable `orden.pagada`
registra una factura `PENDIENTE_AFIP` por compra confirmada; SOAP todavia no esta integrado.
Con una base existente, crear el esquema `facturacion` antes de arrancar; no borrar el volumen.
Guion de consumidor detenido, duplicados y DLQ: [docs/mensajeria.md](docs/mensajeria.md).

## Productoras, invitaciones y compras invitadas

Cualquier persona puede empezar desde **Organizar eventos → Crear mi productora**. Después de ingresar o registrarse, completa el nombre y llega al resumen de su productora, con accesos para preparar el primer evento e invitar al equipo. Quien ya tiene membresías entra por **Panel de organización**; **Crear otra productora** queda dentro de ese panel. El dueño envía invitaciones desde **Equipo**; el invitado usa el mismo email y acepta durante los siete días de vigencia.

Guardar un evento abre su gestión como borrador. La publicación requiere revisar sus datos y confirmar que quedará a la venta. Guion manual y comprobaciones: [Correcciones de los recorridos UX](docs/correcciones-ux-flujos.md).

Los correos quedan en una cola persistente con reintentos. En desarrollo, `docker compose up -d` también levanta Mailpit: su panel está en http://localhost:8025 y el SMTP escucha en `localhost:1025`. Para SMTP real, completar las variables `PASSLY_SMTP_*` en `.env`; no guardar credenciales en el repositorio.

Desde el evento, se puede agregar al carrito sin sesión y pagar como invitado con la tarjeta de prueba vigente. Usa el mismo `ServicioDePagos` que la compra autenticada; los datos de tarjeta solo viven en memoria mientras se completa el formulario. La orden y los QR se consultan desde el enlace privado enviado por email; si se pierde, la pantalla `/orden-invitado` permite emitir uno nuevo con el número de orden y el email de compra. El enlace se invalida al solicitar otro.

La migración aditiva de octubre corre automáticamente antes de Hibernate al iniciar el backend. Conserva órdenes existentes; no hace falta borrar el volumen.

## Control de acceso (PAS-31)

Los usuarios VALIDADOR y ORGANIZADOR tienen **Validar entradas** en el menu. Vera, del perfil demo,
valida entradas de Aurora. La ruta `/validar` permite camara o ingreso manual, con uso unico bajo
concurrencia. La camara necesita HTTPS o localhost. Guion: [docs/accesos.md](docs/accesos.md).
En bases existentes crear `accesos` y `facturacion` antes de arrancar; no borrar datos.
