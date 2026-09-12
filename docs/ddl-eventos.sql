-- DDL de referencia del esquema 'eventos'.
--
-- No se ejecuta: Hibernate genera estas tablas via ddl-auto=update (ver application.yml).
-- Se documenta igual porque es lo que hace defendible la pregunta "y vos sabes que tablas
-- te creo el ORM?", y porque es el punto de partida cuando el proyecto pase a Flyway o a
-- ddl-auto=validate (deuda tecnica declarada: 'update' no versiona el esquema ni sabe
-- borrar columnas).
--
-- El esquema en si (CREATE SCHEMA eventos) lo crea db/init/01-esquemas.sql: Hibernate crea
-- tablas dentro de un esquema, no el esquema.

create table eventos.evento (
    id            bigint generated always as identity primary key,
    productora_id bigint        not null,       -- productoras.productora(id), SIN FK: ver abajo
    nombre        varchar(150)  not null,
    descripcion   text,
    fecha_hora    timestamptz   not null,
    lugar         varchar(200)  not null,
    estado        varchar(20)   not null        -- BORRADOR | PUBLICADO | CANCELADO
);

create index idx_evento_productora on eventos.evento (productora_id);

create table eventos.tipo_entrada (
    id              bigint  generated always as identity primary key,
    evento_id       bigint  not null references eventos.evento(id),
    nombre          varchar(80)   not null,
    precio          numeric(12,2) not null,
    cupo_total      integer not null,
    cupo_disponible integer not null,
    version         bigint  not null default 0,   -- bloqueo optimista (@Version)
    constraint uk_tipo_entrada_nombre_por_evento unique (evento_id, nombre)
);

-- Decisiones detras de estos tipos:
--
-- * id bigint identity, no UUID: legibles en las URLs de la demo con curl. Tickets si va a
--   usar UUID, porque un id de ticket secuencial es adivinable y el QR no puede serlo.
-- * numeric(12,2) + BigDecimal, nunca double: es dinero.
-- * timestamptz + OffsetDateTime: la hora de un evento tiene zona.
-- * estado como varchar (STRING), no un tipo numerico: agregar un estado nuevo no debe
--   corromper las filas existentes (@Enumerated(EnumType.STRING) en la entidad).
-- * productora_id NO lleva foreign key, aunque apunte a productoras.productora(id). Una FK
--   entre esquemas de componentes distintos es exactamente el join que CLAUDE.md 4.8
--   prohibe, y ataria los dos componentes a nivel de motor. La integridad la impone el
--   negocio: antes de crear el evento, Eventos le pregunta a ProductoraService si quien
--   opera puede gestionar esa productora, y esa pregunta da false para una productora
--   inexistente. El indice si esta, porque filtrar la cartelera por productora es una
--   consulta del camino caliente.
-- * productora_id es updatable=false en la entidad: un evento no cambia de dueño.
--   Transferirlo seria una operacion de negocio con sus propias reglas, no un update.
