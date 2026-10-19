-- Idempotente, aditiva. Se ejecuta antes de Hibernate; no elimina datos.
CREATE SCHEMA IF NOT EXISTS notificaciones^^^
CREATE SCHEMA IF NOT EXISTS ventas^^^
CREATE TABLE IF NOT EXISTS ventas.limite_recuperacion (
  clave varchar(64) PRIMARY KEY, ventana timestamptz NOT NULL, cantidad integer NOT NULL
)^^^
DO $$ BEGIN
 IF to_regclass('ventas.orden') IS NOT NULL THEN
   ALTER TABLE ventas.orden ALTER COLUMN comprador_id DROP NOT NULL;
 END IF;
 IF to_regclass('tickets.ticket') IS NOT NULL THEN
   ALTER TABLE tickets.ticket ALTER COLUMN comprador_id DROP NOT NULL;
 END IF;
 IF to_regclass('facturacion.factura') IS NOT NULL THEN
   ALTER TABLE facturacion.factura ALTER COLUMN comprador_id DROP NOT NULL;
   ALTER TABLE facturacion.factura ADD COLUMN IF NOT EXISTS comprador_nombre varchar(150);
   ALTER TABLE facturacion.factura ADD COLUMN IF NOT EXISTS comprador_email varchar(254);
 END IF;
 IF to_regclass('usuarios.usuario') IS NOT NULL THEN
   CREATE UNIQUE INDEX IF NOT EXISTS uk_usuario_email_normalizado ON usuarios.usuario(lower(email));
 END IF;
END $$^^^
