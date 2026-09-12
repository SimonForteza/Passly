-- Un esquema de Postgres por componente de negocio (CLAUDE.md 4.8).
--
-- Hibernate crea las TABLAS dentro de un esquema, pero no crea el esquema en si:
-- si 'eventos' no existe, el arranque falla con SchemaManagementException. Por eso
-- se crea aca, en la inicializacion del contenedor.
--
-- A medida que se implemente cada componente se agrega su esquema a este archivo:
-- ventas, tickets, accesos, notificaciones, facturacion.
--
-- OJO: este script corre SOLO en la primera inicializacion del volumen. Si se agrega un
-- esquema con el volumen ya creado, hay que recrearlo (docker compose down -v) o crear
-- el esquema a mano; si no, Hibernate falla con SchemaManagementException al arrancar.

create schema if not exists eventos;
create schema if not exists usuarios;
create schema if not exists productoras;
