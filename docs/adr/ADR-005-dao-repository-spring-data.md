# ADR-005: DAO con repositorios de Spring Data JPA en cada componente

- Estado: Aceptado
- Fecha: 2026-11-15
- Responsables: Equipo Passly
- Alcance: Capa de datos de todos los componentes del backend

## Contexto

Cada componente guarda sus datos en su propio esquema de PostgreSQL (ADR-002). Su capa de negocio tiene que leer y escribir esos datos sin mezclar reglas de dominio con SQL, conexiones o detalles de Hibernate. Ademas, ninguna entidad puede salir de su componente: las fronteras se cruzan con DTO (CLAUDE.md §4.3).

## Fuerzas y restricciones

- La capa de negocio tiene que poder cambiar sin tocar el acceso a datos, y al reves.
- El acceso a datos de un componente no puede ser visible para otro.
- Las transacciones son declarativas (`@Transactional` en la capa de negocio).
- Algunas operaciones necesitan control fino: uso unico atomico de un ticket, alta idempotente de una factura y bloqueos para evitar condiciones de carrera.
- El equipo es chico y el tiempo, corto. Escribir SQL a mano para cada CRUD no se justifica.

## Alternativas consideradas

### DAO escrito a mano con `JdbcTemplate`

Control total del SQL. Descartada como estrategia general: obliga a mapear filas a objetos y a escribir cada consulta, sin ganar nada en el 90 % de los casos.

### Active Record

La entidad se guarda sola (`evento.save()`). Descartada: mezcla persistencia con dominio, no es el modelo de JPA y dificulta probar las reglas sin base.

### Usar el `EntityManager` desde la capa de negocio

Descartada: la capa de negocio quedaria atada a JPA y repetiria consultas.

### DAO con repositorios de Spring Data JPA

Una interfaz por agregado que extiende `JpaRepository`. Spring genera la implementacion y deja agregar consultas derivadas por nombre, `@Query` o SQL nativo cuando hace falta.

## Decision

Cada componente tiene su capa `internal/datos` con sus entidades JPA y sus repositorios. Hoy son 11:

| Componente | Repositorios |
|---|---|
| eventos | `EventoRepository`, `TipoEntradaRepository` |
| usuarios | `UsuarioRepository` |
| productoras | `ProductoraRepository`, `InvitacionRepository` |
| ventas | `OrdenRepository`, `IntentoCompraRepository` |
| pagos | `PagoRepository` |
| tickets | `TicketRepository` |
| accesos | `ValidacionRepository` |
| facturacion | `FacturaRepository` |

Reglas:

- Solo la capa `internal/negocio` del mismo componente usa sus repositorios. Las entidades se convierten a DTO con un mapper (`EventoMapper`, `VentaMapper`) antes de salir.
- Repositorios y entidades son `public` porque Java no ve entre `internal.negocio` e `internal.datos` si son package-private. La frontera real la verifica Spring Modulith: si otro componente los importa, `EstructuraDeModulosTest` falla (ADR-001).
- Las consultas especiales se escriben en el repositorio, no en el negocio:
  - `TicketRepository`: `UPDATE` condicional `EMITIDO -> USADO`. Dos validaciones simultaneas dan un `VALIDO` y un `YA_USADO`.
  - `FacturaRepository` e `IntentoCompraRepository`: `INSERT ... ON CONFLICT DO NOTHING` para que el alta sea idempotente.
  - `OrdenRepository.findByIdAndCompradorId`: filtra por dueno en la consulta, asi una orden ajena da 404 sin cargarla.
  - `InvitacionRepository`, `ProductoraRepository`, `UsuarioRepository` y `OrdenRepository` usan `@Lock(PESSIMISTIC_WRITE)` donde hay carreras.
  - `TipoEntrada` usa `@Version` (bloqueo optimista) para el descuento de cupo.

## Consecuencias positivas

- La capa de negocio expresa reglas, no SQL.
- Las consultas concurrentes delicadas quedan visibles en un solo lugar por componente.
- Cada repositorio solo ve su esquema, en linea con ADR-002: no hay joins entre esquemas.
- Para probar el negocio se puede reemplazar el repositorio.

## Consecuencias negativas

- Spring Data esconde el SQL generado. Hay que mirar los logs para entender una consulta derivada.
- Los repositorios `public` dependen de Spring Modulith para no usarse desde afuera.
- Las consultas nativas (`ON CONFLICT`) atan esas operaciones a PostgreSQL.
- Las relaciones lazy obligan a leer dentro de una transaccion (`@Transactional(readOnly = true)` en `VentaServiceImpl.consultarOrden`).
- El esquema todavia lo genera Hibernate (`ddl-auto: update`). Migrar a Flyway sigue como deuda (CLAUDE.md §10).

## Evidencia en el repositorio

- `backend/src/main/java/com/passly/*/internal/datos/*Repository.java`: los 11 repositorios.
- `backend/src/main/java/com/passly/tickets/internal/datos/TicketRepository.java`: el `UPDATE` condicional.
- `backend/src/main/java/com/passly/facturacion/internal/datos/FacturaRepository.java`: el `ON CONFLICT`.
- `backend/src/main/java/com/passly/eventos/internal/negocio/EventoMapper.java`: entidad a DTO antes de cruzar la frontera.
- `backend/src/test/java/com/passly/EstructuraDeModulosTest.java`: impide usar estos repositorios desde otro componente.

## Criterio de revision

Revisar si una parte necesita consultas que JPA resuelve mal (reportes pesados o agregaciones), o si un componente pasa a tener una base propia. En ese caso, evaluar un DAO con SQL explicito para esa parte, sin cambiar el resto.
