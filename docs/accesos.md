# Validar entradas en puerta — PAS-31

## Preparacion

Con una base existente, ejecutar `CREATE SCHEMA IF NOT EXISTS accesos;` antes de iniciar el backend.
Hibernate agrega `tickets.ticket.usado_en`, nullable para conservar los tickets emitidos.
No borrar el volumen de Postgres. Para mensajeria, crear tambien el esquema facturacion.

## Demo

1. Iniciar backend con perfil demo y frontend. El seed ya incorpora a Vera (`validador@passly.test`, clave `passly1234`) como VALIDADOR de Aurora.
2. Entrar como comprador y comprar una entrada del Festival de Jazz. Abrir su QR en el telefono o descargarlo.
3. En otro navegador, entrar como Vera y abrir **Validar entradas** (`/validar`). Elegir el evento y activar la camara.
4. Escanear: pantalla verde VALIDO. Repetir: amarillo YA USADO y hora del primer ingreso.
5. Si no hay camara, expandir **Contenido para ingreso manual** en el ticket del comprador, copiar y pegar en el formulario del validador.
6. Probar firma adulterada, otro evento y operador de otra productora con docs/http/09-accesos.http.

La camara requiere HTTPS o localhost. Abrir `http://IP-de-la-PC:5173` desde el telefono no alcanza: usar HTTPS. Sin permisos o sin camara sigue disponible el ingreso manual.
La pantalla detiene las pistas al pausar, mostrar resultado, cambiar evento o salir. Despues de tres segundos vuelve al escaneo; tambien hay un boton para continuar.

## Contratos y decisiones

POST /api/accesos/validaciones recibe `{ idEvento, contenidoQr }`. Responde 200 con `{ resultado, ticket, mensaje }`: VALIDO, YA_USADO, OTRO_EVENTO o INVALIDO.
Ticket solo aparece para el evento seleccionado y contiene id, idEvento, nombreTipoEntrada y usadoEn. No devuelve datos del comprador ni el QR.
401 si no hay autenticacion, 403 por rol/productora, 400 por cuerpo incompleto y 404 si no existe el evento publico. Un evento cancelado no permite consumir entradas.

Spring Security exige VALIDADOR u ORGANIZADOR. Productoras decide la capacidad puedeValidarEntradas: DUENIO, STAFF o VALIDADOR de la productora del evento. El usuario se obtiene del principal, nunca del body.
Tickets verifica firma y pertenencia antes de ejecutar UPDATE condicionado a EMITIDO y al evento. La cantidad de filas actualizadas decide quien gano; un segundo consumidor recibe YA_USADO sin cambiar usadoEn. El consumo y la auditoria comparten transaccion local.
Los intentos autorizados, incluso QR invalidos, se guardan en accesos.validacion. Las solicitudes rechazadas por seguridad no consumen ni crean registros de negocio.

Accesos depende de los contratos de Tickets, Eventos y Productoras; Tickets sigue sin dependencias. No hay joins ni FKs entre esquemas.
Fuera de alcance: resumen opcional, React Native, offline y suscripcion a ticket.emitido.
