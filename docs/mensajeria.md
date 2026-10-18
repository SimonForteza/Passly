# Mensajeria real — PAS-32

## Preparar

1. Ejecutar `docker compose up -d` y abrir http://localhost:8161/console (passly / passly por defecto).
2. Con un volumen Postgres existente, ejecutar `CREATE SCHEMA IF NOT EXISTS facturacion;` antes de arrancar el backend. No hace falta borrar datos. Los scripts de db/init solo corren con un volumen nuevo.
3. Arrancar el backend con el perfil demo y el frontend como indica README.

Artemis usa un volumen propio y la configuracion versionada en infra/artemis/broker.xml.
Las variables PASSLY_BROKER_URL, PASSLY_BROKER_USER y PASSLY_BROKER_PASSWORD configuran la conexion.
La aplicacion no lleva un broker embebido. Las pruebas de integracion si arrancan un Artemis aislado en 61618.

## Demostracion

1. Arrancar el backend con `PASSLY_FACTURACION_CONSUMIDOR_ACTIVO=false`.
2. Comprar una entrada en la web (o usar docs/http/08-ventas.http).
3. En la consola, abrir la cola anycast `orden.pagada`: debe haber un mensaje pendiente, con idOrden, idComprador, total y cobradaEn en JSON.
4. Reiniciar el backend con `PASSLY_FACTURACION_CONSUMIDOR_ACTIVO=true`. El mensaje se consume.
5. Consultar `SELECT * FROM facturacion.factura ORDER BY id DESC;`: una fila PENDIENTE_AFIP por orden.
6. Reenviar el mismo JSON desde la consola: la restriccion unica y ON CONFLICT mantienen una sola factura.
7. Enviar un TextMessage `{"idOrden":-1}`. Tras tres entregas totales con 5 segundos de demora, debe terminar en DLQ. No se inserta factura.
8. Intentar una compra sin cupo: el rollback no publica mensaje.

## Garantias y limites

Ventas publica un evento interno; AFTER_COMMIT envia un TextMessage persistente a la cola durable.
Facturacion tiene su propio record para el JSON: no importa clases de Ventas.
El listener usa la sesion JMS transaccional de Boot y llama al servicio JPA transaccional; el ACK llega despues del commit. Una caida entre commit y ACK puede duplicar la entrega, por eso la insercion es idempotente incluso con consumidores concurrentes.
El puerto ClienteAfip solo registra que falta la integracion SOAP: no existe factura fiscal emitida.

Si falla el envio despues del commit, la orden sigue confirmada, no se devuelve el cobro y se registra ERROR con el id de orden. Puede perderse el mensaje: falta outbox y la recuperacion es manual. No se promete exactly-once de transporte.

El conversor JSON de Framework 7 es JacksonJsonMessageConverter (Jackson 3). Aqui usamos JSON explicito con JsonMapper y TextMessage para evitar headers con nombres de clases y acoplar ambos modulos.
