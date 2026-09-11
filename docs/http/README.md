# Peticiones HTTP de prueba

Para la extension **REST Client** de VS Code (`humao.rest-client`). Cada archivo es
independiente: se abre, aparece un link `Send Request` arriba de cada `###`, y se clickea.

| Archivo | Que prueba |
|---|---|
| [01-salud.http](01-salud.http) | `/actuator/health` (publico) y `/actuator/modulith` (autenticado) |
| [02-cartelera.http](02-cartelera.http) | `GET /api/eventos` (cartelera publica) |
| [03-flujo-feliz.http](03-flujo-feliz.http) | crear -> consultar -> publicar -> cartelera -> **republicar (409)** -> disponibilidad |
| [04-errores.http](04-errores.http) | evento inexistente (404), evento invalido (400) |
| [05-seguridad.http](05-seguridad.http) | matriz de autorizacion por rol (PAS-6): 401 / 403 / 201-200 |

## Autenticacion (PAS-6)

La API usa **HTTP Basic**. Los endpoints publicos no piden credenciales: `GET /actuator/health`,
la cartelera (`GET /api/eventos`, `GET /api/eventos/{id}`) y el alta publica (`POST /api/usuarios`,
que solo puede crear `COMPRADOR`). Todo lo demas exige estar autenticado, y las operaciones
sensibles exigen ademas un rol (`@PreAuthorize`): crear/publicar eventos -> `ORGANIZADOR`; crear
usuarios privilegiados y listar usuarios -> `ADMIN`.

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
docker compose up -d
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

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
docker exec passly-db psql -U passly -d passly -c "truncate table eventos.tipo_entrada, eventos.evento restart identity cascade;"
```
