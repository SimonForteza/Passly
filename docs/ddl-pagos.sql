-- DDL de referencia del esquema 'pagos'.
--
-- No se ejecuta: Hibernate genera esta tabla via ddl-auto=update (ver application.yml).
-- Se documenta igual por el mismo motivo que docs/ddl-eventos.sql: es el punto de partida
-- cuando el proyecto pase a Flyway o a ddl-auto=validate (deuda tecnica declarada).
--
-- El esquema en si (CREATE SCHEMA pagos) lo crea db/init/01-esquemas.sql.

create table pagos.pago (
    id                      bigint generated always as identity primary key,
    monto                   numeric(12,2) not null,
    moneda                  varchar(3)    not null,        -- ISO 4217, ej. ARS
    referencia              varchar(100)  not null,        -- de quien pidio el cobro
    id_transaccion_externa  varchar(100)  not null,        -- id que asigna la pasarela
    estado                  varchar(20)   not null,        -- APROBADO | RECHAZADO | REVERTIDO
    fecha_procesamiento     timestamptz   not null,
    id_transaccion_reversion varchar(100),                 -- null hasta que se revierte
    fecha_reversion         timestamptz                     -- idem
);

create index idx_pago_referencia on pagos.pago (referencia);

-- Decisiones detras de esta tabla:
--
-- * NO hay columnas de tarjeta (numero, vencimiento, codigo de seguridad). Son datos
--   transitorios que solo existen para armar el pedido a la pasarela externa y se descartan
--   apenas se usan -- el mismo criterio que 'usuarios' aplica a la contraseña hasheada.
-- * Se persiste tanto el cobro aprobado como el rechazado: permite conciliar contra el
--   resumen de la pasarela, y deja evidencia de que hubo una llamada HTTP real.
-- * numeric(12,2) + BigDecimal, nunca double: es dinero.
-- * timestamptz + OffsetDateTime: igual que fecha_hora en eventos.evento.
-- * estado como varchar (STRING), no un tipo numerico: agregar un estado nuevo no debe
--   corromper las filas existentes (@Enumerated(EnumType.STRING) en la entidad).
-- * El indice sobre referencia es el camino caliente de una futura conciliacion por orden.
-- * id_transaccion_reversion / fecha_reversion quedan en null mientras el pago no se revierte:
--   la reversion es una operacion posterior y opcional sobre un pago ya APROBADO, no un dato
--   que exista desde el alta de la fila.
