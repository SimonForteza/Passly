# Peticiones HTTP de prueba

Para la extension **REST Client** de VS Code (`humao.rest-client`). Cada archivo es
independiente: se abre, aparece un link `Send Request` arriba de cada `###`, y se clickea.

| Archivo | Que prueba |
|---|---|
| [01-salud.http](01-salud.http) | `/actuator/health` y `/actuator/modulith` |
| [02-cartelera.http](02-cartelera.http) | cartelera publica, filtro por productora, listado de productoras |
| [03-flujo-feliz.http](03-flujo-feliz.http) | crear -> consultar -> publicar -> cartelera -> **republicar (409)** -> disponibilidad |
| [04-errores.http](04-errores.http) | 404, 400 por campo, **401 sin identidad**, 403 por productora inexistente |
| [05-productoras.http](05-productoras.http) | alta de productoras, padron de miembros y el cruce de los dos ejes de rol |
| [06-aislamiento.http](06-aislamiento.http) | **el guion central: una productora no toca las fiestas de otra** |

## Antes de correrlas

```bash
docker compose down -v && docker compose up -d
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

El `down -v` recrea el volumen. Hace falta la primera vez despues de un cambio de esquema:
`db/init/` solo corre en la inicializacion del volumen, y con `ddl-auto: update` Hibernate
no agrega una columna `not null` a una tabla que ya tiene filas. Despues, `up -d` alcanza.

## Sobre el header `X-Usuario-Id`

Las operaciones de gestion llevan `X-Usuario-Id`, que es **quien opera**. Es temporal:
Spring Security es PAS-6. Es deliberadamente falsificable y no pretende ser seguridad —
lo que logra es que el modelo de autorizacion ya este completo y probado para cuando
llegue la autenticacion de verdad. Migrar sera una linea por endpoint, sin tocar ningun
DTO ni ninguna firma de servicio.

La lectura publica (cartelera, ficha de productora) no lo pide, que es justamente el
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
