# ADR-006: Patron State para el ciclo de vida del evento

- Estado: Aceptado
- Fecha: 2026-11-15
- Responsables: Equipo Passly
- Alcance: Componente `eventos`

## Contexto

Un evento pasa por tres estados: `BORRADOR`, `PUBLICADO` y `CANCELADO`. Lo que se puede hacer depende del estado (PAS-19):

| Operacion | BORRADOR | PUBLICADO | CANCELADO |
|---|---|---|---|
| Publicar | si | 409 | 409 |
| Cancelar | si | si | 409 |
| Editar datos | si | 409 | 409 |
| Editar un tipo de entrada | si | 409 | 409 |
| Agregar un tipo de entrada | si | si | 409 |
| Ampliar cupo | si | si | 409 |
| Vender entradas | no | si | no |

Hasta PAS-35 esas reglas eran `if (estado == ...)` repartidos por la entidad `Evento`. Cada operacion nueva o estado nuevo obligaba a revisar todos los `if`, y no habia forma de cancelar un evento. La consigna del 16/11 pide el patron State en la entidad central, con 409 ante una transicion invalida.

## Fuerzas y restricciones

- La matriz de PAS-19 no puede cambiar: es lo que ya usan la web y los tests.
- El estado tiene que seguir guardado en la columna `eventos.evento.estado`, sin migrar datos.
- `EstadoEvento` es parte del contrato publico: viaja en `EventoDTO` y lo lee el frontend.
- Agregar un estado nuevo deberia ser imposible de hacer a medias.
- Una operacion invalida responde 409 con `ProblemDetail`.

## Alternativas consideradas

### Enum con `if` o `switch` en la entidad

Lo que habia. Es simple, pero la regla de cada estado queda repartida en cada metodo de `Evento`, y olvidar un caso no da error de compilacion.

### Metodos abstractos en el enum publico

Cada constante del enum implementa su comportamiento. Descartada: `EstadoEvento` esta en el contrato que ven los demas modulos, y meterle reglas de negocio las expone fuera del componente.

### Persistir el objeto de estado con un `@Convert`

Guardar el nombre de la clase de estado en la base. Descartada: ata las filas al nombre de una clase Java. Un refactor romperia los datos guardados.

### State con el enum como dato persistido

Una interfaz con una clase por estado. La entidad delega en el estado actual, que se resuelve desde el enum guardado.

## Decision

En `eventos.internal.datos`:

- `EstadoDeEvento` es una interfaz `sealed` (`permits Borrador, Publicado, Cancelado`) con una operacion por accion del ciclo de vida: `publicar`, `cancelar`, `editarDatos`, `agregarTipoEntrada`, `editarTipoEntrada`, `ampliarCupo` y `admiteVentas`.
- `Borrador`, `Publicado` y `Cancelado` son singletons sin atributos y package-private. Cada uno implementa lo que admite y, para lo que no, lanza `TransicionDeEstadoInvalidaException` o `EdicionDeEventoInvalidaException`. `ManejadorDeErrores` las traduce a **409**.
- `EstadoDeEvento.de(EstadoEvento)` traduce el dato guardado al comportamiento con un `switch` exhaustivo **sin `default`**: si alguien agrega un valor a `EstadoEvento` sin su clase, el codigo no compila.
- `Evento` sigue guardando el enum (`@Enumerated(STRING)`). Cada operacion delega en `EstadoDeEvento.de(estado)`. Los estados aplican el cambio con mutadores package-private de `Evento` (`pasarA`, `aplicarDatos`...), que no vuelven a chequear el estado. Por eso viven en el mismo paquete que la entidad.
- Se agrego la transicion que faltaba: `POST /api/eventos/{id}/cancelacion`, con la misma autorizacion que publicar (rol `ORGANIZADOR` y membresia en la productora).
- `admiteVentas()` lo consultan `EventoService.consultarDisponibilidad` y `descontarCupo`. Asi, no se puede confirmar una compra de un borrador ni de un evento cancelado.

## Consecuencias positivas

- Cada estado se lee en una sola clase: lo que admite y lo que rechaza.
- Agregar un estado sin su comportamiento no compila (`sealed` + `switch` exhaustivo).
- El esquema y el contrato publico no cambiaron.
- La matriz completa se prueba sin Spring ni base de datos (`MatrizDeEstadosDeEventoTest`, 18 combinaciones).
- El chequeo de ventas quedo en el mismo lugar que el resto de las reglas del estado.

## Consecuencias negativas

- Hay mas archivos para tres estados. Con tan pocos estados, un `switch` tambien se entenderia. El patron se justifica por la cantidad de operaciones (siete) y por el requisito de la consigna.
- Los mutadores package-private de `Evento` no validan el estado. Cualquier clase de `internal.datos` podria llamarlos salteando las reglas. Lo mitiga que el paquete es chico y solo tiene entidades, repositorios y estados.
- Reintegros y aviso a los compradores al cancelar quedan fuera de alcance (HU-14).

## Evidencia en el repositorio

- `backend/src/main/java/com/passly/eventos/internal/datos/EstadoDeEvento.java`, `Borrador.java`, `Publicado.java`, `Cancelado.java` y `Evento.java`.
- `backend/src/main/java/com/passly/eventos/EstadoEvento.java`: el enum publico, sin comportamiento.
- `backend/src/test/java/com/passly/eventos/internal/datos/MatrizDeEstadosDeEventoTest.java`: test unitario de la matriz completa.
- `backend/src/test/java/com/passly/eventos/internal/web/CancelacionDeEventoTest.java`: cancelar dos veces da 200 y despues 409, y otra productora recibe 403.
- `docs/http/03-flujo-feliz.http`: republicar da 409.

## Criterio de revision

Revisar si aparecen estados con datos propios (por ejemplo, `POSTERGADO` con fecha nueva) o transiciones automaticas por fecha (`FINALIZADO`). En el primer caso, los estados dejarian de ser singletons. En el segundo, habria que decidir quien dispara la transicion.
