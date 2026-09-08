-- Un esquema de Postgres por componente de negocio (CLAUDE.md 4.8).
--
-- Hibernate crea las TABLAS dentro de un esquema, pero no crea el esquema en si:
-- si 'eventos' no existe, el arranque falla con SchemaManagementException. Por eso
-- se crea aca, en la inicializacion del contenedor.
--
-- A medida que se implemente cada componente se agrega su esquema a este archivo:
-- ventas, tickets, accesos, notificaciones, facturacion.

create schema if not exists eventos;
create schema if not exists usuarios;
