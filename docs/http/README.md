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
| [07-seguridad.http](07-seguridad.http) | matriz de autorizacion por rol (PAS-6): 401 / 403 / 201-200 |

## Autenticacion (PAS-6)

La API usa **HTTP Basic**. Los endpoints publicos no piden credenciales: `GET /actuator/health`,
la cartelera (`GET /api/eventos`, `GET /api/eventos/{id}`) y el alta publica (`POST /api/usuarios`,
que solo puede crear `COMPRADOR`). Todo lo demas exige estar autenticado, y las operaciones
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

En los `.http`, REST Client arma el header a partir de `Authorization: Basic usuario contrasena`
(separados por un espacio). Con `curl`, el equivalente es `-u usuario:contrasena`.

## Antes de correrlas

```bash
docker compose down -v && docker compose up -d
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

El `down -v` recrea el volumen. Hace falta la primera vez despues de un cambio de esquema:
`db/init/` solo corre en la inicializacion del volumen, y con `ddl-auto: update` Hibernate
no agrega una columna `not null` a una tabla que ya tiene filas. Despues, `up -d` alcanza.

## Como se resuelve "quien opera"

Eventos y Productoras resuelven distinto la identidad de quien opera, y a proposito quedan
en momentos distintos de la migracion:

- **Eventos** ya usa Spring Security: `Authorization: Basic` mas `@PreAuthorize`. El
  `UserDetails` que arma el modulo `seguridad` usa el **id numerico** del usuario como
  username (no el email), asi que el controller lee `Authentication#getName()` para
  obtener el id del actuante sin depender de Usuarios para resolverlo — dependencia que el
  `package-info` de Eventos no declara y que haria fallar el build (CLAUDE.md 4.4).
- **Productoras** todavia usa el header temporal `X-Usuario-Id`: es deliberadamente
  falsificable y no pretende ser seguridad. Migrarlo es aplicar el mismo patron que ya se
  uso en Eventos.

La lectura publica (cartelera, ficha de productora) no pide identidad, que es justamente el
punto de un marketplace.

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
