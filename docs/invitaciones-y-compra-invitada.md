# Invitaciones y compra como invitado

## Productoras

1. Elegir **Organizar eventos → Crear mi productora** e ingresar o crear una cuenta. Se conserva el destino del alta.
2. Completar el nombre comercial y, de forma opcional, CUIT, descripción y logo. El éxito abre **Resumen**, con accesos al primer evento y a **Equipo**. Para una cuenta con membresías, la entrada habitual es **Panel de organización**.
3. El alta agrega al creador como `DUENIO` y actualiza su capacidad global a `ORGANIZADOR` en la misma transacción.
4. Desde **Equipo**, el dueño envía una invitación a `STAFF` o `VALIDADOR`. Un staff puede gestionar eventos; quien valida solo puede controlar entradas de esa productora. La aceptación abre la tarea correspondiente y muestra una confirmación.
5. La invitación dura siete días y se consume una sola vez. El link lleva el token en el fragmento de URL, de modo que el navegador lo envía por un encabezado API y no en el `Referer`. La persona debe usar la cuenta asociada al email invitado.

El endpoint legado de alta directa de miembros permanece disponible para clientes previos y sigue exigiendo un usuario registrado y rol compatible. Las invitaciones nuevas usan `/api/productoras/{id}/invitaciones`; el dueño puede consultar estado de SMTP, reenviar (se invalida el link anterior) o revocar.

## Correo local y SMTP

`docker compose up -d` levanta Mailpit en `http://localhost:8025` y SMTP en el puerto `1025`. SMTP real usa `PASSLY_SMTP_HOST`, `PASSLY_SMTP_PORT`, `PASSLY_SMTP_USER`, `PASSLY_SMTP_PASSWORD`, `PASSLY_SMTP_AUTH`, `PASSLY_SMTP_TLS` y `PASSLY_CORREO_REMITENTE`. Los emails se guardan en `notificaciones.correo`, se envían fuera de la transacción que crea la invitación o la compra y se reintentan con espera creciente.

## Compra sin cuenta

1. Agregar entradas desde una ficha pública; la selección vive en una sesión separada del carrito autenticado.
2. En `/carrito`, completar nombre y email y confirmar como invitado, o ingresar y transferir la selección a la cuenta. Ambos recorridos muestran evento, fecha, lugar y total. La interfaz explica que el pago vigente es simulado. El servidor agrupa y vuelve a validar disponibilidad antes del pago; la transacción descuenta cupo, crea orden y emite los QR existentes.
3. La respuesta inmediata abre la orden. El email también contiene un enlace privado; sus tickets pueden descargarse en PNG o mostrarse en la puerta. El acceso vence 30 días después del último evento de la compra.
4. Para recuperar el enlace, ingresar número de orden y email en `/orden-invitado`. La respuesta es genérica tanto si coincide como si no; al emitir otro enlace se revoca el anterior. Hay límites por hora por dirección y por orden/email.

El intento conserva una clave de idempotencia dentro de la sesión del comprador. Un rechazo conocido permite un intento nuevo. Si el estado del cobro resulta ambiguo por una falla del proveedor, la API bloquea nuevos cobros con esa clave y el caso requiere conciliación antes de generar otra.

La migración `backend/src/main/resources/db/migraciones/20261018-altas-invitados.sql` agrega las tablas e índices necesarios y permite `comprador_id` nulo en órdenes invitadas, sus tickets y sus facturas. El nombre y email se copian a la orden como datos históricos. No se guarda el token de acceso en texto plano.

## Pruebas

`AltasInvitadosIntegrationTest` cubre alta, roles, permisos, expiración, revocación, reenvío, concurrencia, compra invitada, cupo, pago rechazado, idempotencia, QR, recuperación, límites y origen cruzado. `DespachadorDeCorreoTest` cubre recuperación del despacho cuando SMTP vuelve a estar disponible. Ejecutar `cd backend; ./mvnw test` y `cd frontend; npm run build`.
