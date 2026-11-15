# Peticiones HTTP de prueba

Para la extension **REST Client** de VS Code (`humao.rest-client`). Cada archivo es
independiente: se abre, aparece un link `Send Request` arriba de cada `###`, y se clickea.

| Archivo | Que prueba |
|---|---|
| [01-salud.http](01-salud.http) | `/actuator/health` (publico) y `/actuator/modulith` (autenticado) |
| [02-cartelera.http](02-cartelera.http) | cartelera publica, filtro por productora, listado de productoras |
| [03-flujo-feliz.http](03-flujo-feliz.http) | crear -> consultar -> publicar -> cartelera -> **republicar (409)** -> disponibilidad |
| [04-errores.http](04-errores.http) | 404, 400 por campo, **401 sin identidad**, 403 por productora inexistente |
| [05-productoras.http](05-productoras.http) | alta de productoras, padron de miembros y el cruce de los dos ejes de rol |
| [06-aislamiento.http](06-aislamiento.http) | **el guion central: una productora no toca las fiestas de otra** |
| [07-seguridad.http](07-seguridad.http) | matriz de autorizacion por rol (PAS-6): 401 / 403 / 201-200, y `GET /api/usuarios/me`, el "quien soy" del login (PAS-15) |
| [08-ventas.http](08-ventas.http) | carrito stateful, confirmar compra **con tarjeta** (PAS-29, cobra via `PagoService`), **un ticket con QR por unidad**, el rollback (dos lineas, la segunda sin cupo -> 409, **el cobro ya aprobado se revierte de verdad**), y 402/503 segun la tarjeta |
| [09-pagos.http](09-pagos.http) | Adapter REST hacia la pasarela de pago (PAS-7): cobro aprobado, **rechazado (402)**, **pasarela caida (503)**, 401, validacion, y la **reversion** de un cobro (PAS-29): 200, **409 si se repite**, 404 si el pago no existe |
| [10-edicion-eventos.http](10-edicion-eventos.http) | edicion de evento y tipos de entrada (PAS-19): editar en BORRADOR, **comprar un borrador -> 409** (PAS-35), editar en PUBLICADO -> 409, agregar tipo y ampliar cupo en PUBLICADO -> 200/201, nombre repetido -> 409, aislamiento -> 403, sin identidad -> 401 |
| [11-link-compartible.http](11-link-compartible.http) | el link compartible de un evento (PAS-25): publicado -> 200 para todos; **borrador -> 404 (no 403)** para anonimos, compradores y organizadores de otra productora; 200 para quien gestiona la productora dueña |
| [12-ventas-invitados.http](12-ventas-invitados.http) | compra como invitado con `ServicioDePagos`: tarjeta aprobada, rechazada y caída ambigua; consulta de la orden solo con token privado |
| [13-cancelacion-evento.http](13-cancelacion-evento.http) | patron State y cancelacion (PAS-35): publicar -> cancelar (200), **cancelar dos veces -> 409** y **publicar un cancelado -> 409** con ProblemDetail, ampliar cupo de un cancelado -> 409, disponibilidad en `false`, cancelar un borrador -> 200, otra productora -> 403, sin identidad -> 401 |

## Autenticacion (PAS-6, JWT desde PAS-33)

La API usa **JWT**: `POST /api/auth/login` con `{ "email", "password" }` devuelve
`{ token, tipo: "Bearer", expiraEn, usuario }`, y el resto de los requests manda
`Authorization: Bearer <token>`. El token dura 2 h (`PASSLY_JWT_DURACION`). HTTP Basic ya no se
acepta. Los endpoints publicos no piden credenciales: `GET /actuator/health`,
la cartelera (`GET /api/eventos`, `GET /api/eventos/{id}` -- este ultimo con una salvedad: un
borrador solo lo ve quien gestiona su productora, ver `11-link-compartible.http`) y el alta publica (`POST /api/usuarios`,
que solo puede crear `COMPRADOR`), ademas del propio login. Todo lo demas exige estar autenticado, y las operaciones
sensibles exigen ademas un rol (`@PreAuthorize`): crear/publicar eventos -> `ORGANIZADOR` **que
ademas pueda gestionar la productora indicada** (PAS-13); crear usuarios privilegiados y listar
usuarios -> `ADMIN`.

El perfil `demo` siembra un usuario por rol, todos con contrasena `passly1234`:

| Rol | Usuario |
|---|---|
| COMPRADOR | `comprador@passly.test` |
| ORGANIZADOR | `organizador@passly.test` |
| VALIDADOR | `validador@passly.test` |
| ADMIN | `admin@passly.test` |

En los `.http`, cada archivo empieza con un bloque **Login**: uno o mas requests con
`# @name loginX` que hay que correr primero. El resto los referencia con
`Authorization: Bearer {{loginX.response.body.$.token}}`. Con `curl`:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"comprador@passly.test","password":"passly1234"}' | jq -r .token)
curl http://localhost:8080/api/usuarios/me -H "Authorization: Bearer $TOKEN"
```

Un token adulterado, vencido, firmado con otra clave o con `alg: none` da **401**, incluso en una
ruta publica (el filtro valida todo token que recibe). Casos cubiertos en `AutenticacionJwtTest`.

## Antes de correrlas

```bash
docker compose down -v && docker compose up -d
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

El `down -v` recrea el volumen. Hace falta la primera vez despues de un cambio de esquema:
`db/init/` solo corre en la inicializacion del volumen, y con `ddl-auto: update` Hibernate
no agrega una columna `not null` a una tabla que ya tiene filas. Despues, `up -d` alcanza.

## Como se resuelve "quien opera"

Eventos y Productoras resuelven la identidad de quien opera de la misma forma: no hay ningun
header propio, y la resuelve Spring Security a partir del JWT. El modulo `seguridad` pone el
**id numerico** del usuario como `sub` del token y como nombre de la `Authentication` (no el
email), asi que cada controller lee `Authentication#getName()` para obtener el id del
actuante sin depender de Usuarios para resolverlo — dependencia que ninguno de los dos
`package-info` declara y que haria fallar el build (CLAUDE.md 4.4).

Productoras uso hasta hace poco un header temporal (`X-Usuario-Id`), deliberadamente
falsificable, para poder probar el modelo de autorizacion antes de que existiera Spring
Security. Migro con el mismo patron que ya se habia usado en Eventos, sin cambiar ningun DTO
ni ninguna firma de `ProductoraService`: la identidad siempre viajo como parametro aparte,
nunca dentro del request.

La lectura publica (cartelera, ficha de productora) no pide identidad, que es justamente el
punto de un marketplace.

Usuarios expone ademas `GET /api/usuarios/me` (PAS-15): con el mismo mecanismo
(`Authentication#getName()` = id) devuelve el `UsuarioDTO` del autenticado, rol incluido. Con
Basic era lo que usaba el login de la app web; desde PAS-33 el login es `POST /api/auth/login`,
que ya devuelve el `UsuarioDTO`, y `/me` queda como "quien soy" con un token ya emitido.

**El rol se lee en cada request, no del token.** Crear una productora o aceptar una invitacion
cambia el rol global en el acto; si la autorizacion usara el claim `rol` del token, habria que
volver a iniciar sesion para que el cambio valga. El token prueba quien es; el rol vigente sale
de Usuarios (`ConvertidorDeJwtAAutenticacion`).

## El carrito de Ventas necesita cookies (08-ventas.http)

A diferencia de todo lo demas en este directorio, el carrito de `ServicioDeVentas` (PAS-8) es
**estado conversacional en memoria**, atado a la sesion HTTP (`@SessionScope`), no solo al token. REST Client mantiene el `JSESSIONID` entre requests del mismo archivo por
defecto (`rest-client.rememberCookiesForSubsequentRequests`).

Con `curl` a mano hace falta `-b`/`-c` con el mismo archivo de cookies en **todas** las
llamadas del flujo del carrito, y ademas empezar de una cookie vacia: Spring Security cambia
el `JSESSIONID` en **cada** request autenticado (proteccion contra session fixation; verificado
que sigue pasando con JWT, PAS-33), asi que
la cookie de la respuesta anterior es la unica valida para la siguiente -- una de dos
respuestas atras ya no sirve, aunque la sesion siga viva del lado del servidor. Es un detalle
no obvio que vale la pena poder explicar en el oral.

## Ids del perfil demo

Con la base recien creada, el seeder deja estos ids. Si no coinciden, salen de
`GET /api/usuarios` y `GET /api/productoras`.

| | id | |
|---|---|---|
| Carla Compradora | 1 | `COMPRADOR` |
| Ada Admin | 2 | `ADMIN` |
| Omar Organizador | 3 | `ORGANIZADOR`, **DUENIO** de Aurora |
| Olga Organizadora | 4 | `ORGANIZADOR`, **DUENIO** de Nocturna |
| Vera Validadora | 5 | `VALIDADOR`, **VALIDADOR** en Aurora |
| Aurora Producciones | 1 | eventos 1 (Jazz, publicado) y 3 (Recital, **borrador**) |
| Nocturna Live | 2 | evento 2 (Tech, publicado) |

## Como encadenar pedidos (03-flujo-feliz.http)

REST Client permite nombrar una request con `# @name nombre` y reusar su respuesta en las
siguientes con `{{nombre.response.body.$.campo}}` (JSONPath) -- pero **solo dentro del mismo
archivo**, y solo despues de haber ejecutado esa request al menos una vez en la sesion actual
de VS Code. Por eso el flujo completo (crear, publicar, republicar, disponibilidad) esta en un
solo archivo: `crearEvento` corre primero y las siguientes requests usan su respuesta para
completar la URL, sin que haya que copiar ids a mano.

Si se corre `03-flujo-feliz.http` mas de una vez, cada corrida crea un evento nuevo -- no hace
falta resetear nada entre corridas. Para arrancar de cero de todos modos:

```bash
docker compose down -v && docker compose up -d
```

## Demostrar que las fronteras estan verificadas

El argumento mas fuerte del proyecto no se muestra con curl sino con el build. Comentar la
anotacion de cualquier `package-info.java` de un modulo y correr:

```bash
cd backend && ./mvnw test
```

falla con `Module 'eventos' depends on ... Allowed targets: none`. La frontera entre
componentes no es una convencion del equipo: es una condicion de compilacion.

- `09-accesos.http`: compra, consulta de contenido QR, cuatro resultados de validacion y rechazo por rol/productora.
