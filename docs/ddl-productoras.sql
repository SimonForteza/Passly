-- DDL de referencia del esquema 'productoras'.
--
-- No se ejecuta: Hibernate genera estas tablas via ddl-auto=update (ver application.yml).
-- Se documenta igual porque es lo que hace defendible la pregunta "y vos sabes que tablas
-- te creo el ORM?", y porque es el punto de partida cuando el proyecto pase a Flyway o a
-- ddl-auto=validate (deuda tecnica declarada).
--
-- El esquema en si (CREATE SCHEMA productoras) lo crea db/init/01-esquemas.sql: Hibernate
-- crea tablas dentro de un esquema, no el esquema.

create table productoras.productora (
    id               bigint generated always as identity primary key,
    nombre_comercial varchar(150) not null unique,
    cuit             varchar(11)  unique,          -- nullable: ver abajo
    descripcion      text,
    logo_url         varchar(500),
    creada_en        timestamptz  not null
);

create table productoras.miembro (
    id            bigint generated always as identity primary key,
    productora_id bigint      not null references productoras.productora(id),
    usuario_id    bigint      not null,            -- usuarios.usuario(id), SIN FK: ver abajo
    rol_interno   varchar(20) not null,            -- DUENIO | STAFF | VALIDADOR
    agregado_en   timestamptz not null,
    constraint uk_miembro_usuario_por_productora unique (productora_id, usuario_id)
);

-- Decisiones detras de este esquema:
--
-- * productora_id EN miembro SI lleva foreign key, porque las dos tablas son del mismo
--   componente. usuario_id NO la lleva, porque cruza al esquema de Usuarios: una FK entre
--   esquemas de componentes distintos es el join que CLAUDE.md 4.8 prohibe. La integridad
--   la impone el negocio, que antes de incorporar a alguien le pregunta a UsuarioService
--   si existe y si su rol global admite el rol interno pedido.
--
-- * La comparacion entre las dos columnas de la misma tabla es, justamente, el argumento
--   para el oral: la regla no es "no usamos foreign keys", es "no unimos esquemas de
--   componentes distintos".
--
-- * unique(productora_id, usuario_id): nadie puede estar dos veces en el mismo padron. El
--   servicio ademas lo comprueba explicitamente, para devolver un 409 de dominio en vez de
--   una violacion de integridad 500; la constraint queda como red ante escrituras
--   concurrentes.
--
-- * nombre_comercial unique: es la identidad publica de la productora en la cartelera. Si
--   dos pudieran llamarse igual, el comprador no podria distinguir de quien es cada fiesta,
--   que es el problema que este componente resuelve.
--
-- * cuit nullable pero unique: una productora puede operar antes de tener los datos
--   fiscales cargados. En Postgres, unique permite multiples NULL, asi que las dos cosas
--   conviven. Se guardan solo los 11 digitos, sin guiones, para no tener el mismo CUIT en
--   dos formatos distintos y romper la unicidad sin que se note.
--
-- * rol_interno como varchar (STRING), no un tipo numerico: agregar un rol nuevo no debe
--   corromper las filas existentes (@Enumerated(EnumType.STRING) en la entidad).
--
-- * No hay repositorio de Miembro: no es raiz de agregado. Darle uno propio permitiria
--   cargar y guardar membresias salteandose a la productora, que es justo lo que el
--   agregado existe para evitar.
