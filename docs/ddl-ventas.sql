-- DDL de referencia del esquema 'ventas'.
--
-- No se ejecuta: Hibernate genera estas tablas via ddl-auto=update (ver application.yml).
-- Se documenta igual por la misma razon que ddl-eventos.sql: hace defendible la pregunta
-- "y vos sabes que tablas te creo el ORM?", y es el punto de partida cuando el proyecto
-- pase a Flyway o a ddl-auto=validate (deuda tecnica declarada).
--
-- El esquema en si (CREATE SCHEMA ventas) lo crea db/init/01-esquemas.sql.

create table ventas.orden (
    id                bigint        generated always as identity primary key,
    comprador_id      bigint        not null,       -- usuarios.usuario(id), SIN FK: ver abajo
    total             numeric(12,2) not null,
    comprobante_cobro varchar(100)  not null,        -- referencia a la pasarela, no el registro del cobro
    cobrada_en        timestamptz   not null,
    creada_en         timestamptz   not null
);

create table ventas.item_orden (
    id                  bigint        generated always as identity primary key,
    orden_id            bigint        not null references ventas.orden(id),
    tipo_entrada_id     bigint        not null,      -- eventos.tipo_entrada(id), SIN FK: ver abajo
    evento_id           bigint        not null,      -- eventos.evento(id), SIN FK
    nombre_tipo_entrada varchar(80)   not null,       -- snapshot: no cambia si el organizador ajusta el nombre despues
    precio_unitario     numeric(12,2) not null,       -- snapshot: no cambia si el organizador ajusta el precio despues
    cantidad            integer       not null
);

create index idx_item_orden_orden on ventas.item_orden (orden_id);

-- Decisiones detras de este esquema:
--
-- * comprador_id, tipo_entrada_id y evento_id NO llevan foreign key, aunque apunten a
--   usuarios.usuario(id) y eventos.*(id). Una FK entre esquemas de componentes distintos es
--   exactamente el join que CLAUDE.md 4.8 prohibe, y ataria ventas al motor de eventos y
--   usuarios. La FK de item_orden -> orden si esta: es interna al mismo esquema, dentro del
--   mismo agregado, y esa union CLAUDE.md 4.8 la permite explicitamente.
-- * nombre_tipo_entrada y precio_unitario son un SNAPSHOT, no una referencia en vivo. Una
--   orden es un documento historico: si el organizador cambia el precio de un tipo de
--   entrada despues de la compra, lo que se cobro no cambia. Es lo que permite ademas que
--   ventas nunca necesite leer el esquema eventos para mostrar una orden ya confirmada.
-- * Sin tabla ventas.pago. comprobante_cobro y cobrada_en son una REFERENCIA al cobro
--   aprobado por la pasarela (hoy PasarelaDePagoSimulada, mas adelante ServicioDePagos en
--   PAS-7), no el registro del cobro en si -- ese registro va a vivir en el esquema de
--   ServicioDePagos cuando exista. Crear ventas.pago hoy seria crear algo que despues
--   habria que borrar o duplicar.
-- * numeric(12,2) + BigDecimal, nunca double: es dinero, igual que en eventos.
-- * timestamptz + OffsetDateTime: cobrada_en y creada_en tienen zona.
-- * id bigint identity, no UUID: legibles en las URLs de la demo con curl, igual que en
--   eventos. A diferencia de un ticket (que todavia no existe: PAS-8 stubea su emision),
--   el id de una orden no necesita ser impredecible.
-- * No hay tabla para el carrito: es estado conversacional en memoria
--   (com.passly.ventas.internal.negocio.CarritoDeCompra, @SessionScope), no estado de
--   negocio persistido. Persistirlo lo volveria stateless -- trade-off declarado en
--   CLAUDE.md 4.7, elegido a proposito para demostrar gestion de ciclo de vida por
--   contenedor.
