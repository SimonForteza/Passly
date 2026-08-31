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
    id          bigint generated always as identity primary key,
    nombre      varchar(150)  not null,
    descripcion text,
    fecha_hora  timestamptz   not null,
    lugar       varchar(200)  not null,
    estado      varchar(20)   not null          -- BORRADOR | PUBLICADO | CANCELADO
);

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
