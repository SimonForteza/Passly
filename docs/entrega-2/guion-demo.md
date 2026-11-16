# Passly · Guion de la demo del 16/11

**15 minutos** · 14 de recorrido y 1 de margen. Confirmar el turno del grupo en Teams y llegar con el entorno listo.

## Antes de proyectar

- Ensayar el 15/11 por la noche con los cuatro, sobre el mismo commit de `main` que van a presentar. Anotar el commit con `git rev-parse --short HEAD` y registrar asistentes, resultado y defectos en el [plan de entregas](../plan-entregas.md) y el [registro de defectos](../defectos.md).
- Levantar una base descartable. En el ensayo, usar `docker compose down -v` solo si los volúmenes del proyecto son exclusivos de esa demo. Desde la raíz ejecutar `docker compose up -d --wait`; desde `backend/`, `./mvnw spring-boot:run -Dspring-boot.run.profiles=demo`; desde `frontend/`, `npm ci` y `npm run dev`. En Windows usar `./mvnw.cmd`. Confirmar [health UP](http://localhost:8080/actuator/health).
- Abrir el [link de Jazz](http://localhost:5173/eventos/1) en un perfil limpio y dejar [la pantalla de validación](http://localhost:5173/validar) en otro perfil, con Vera dentro. Si el id de Jazz es distinto, obtenerlo con [la cartelera de la API](http://localhost:8080/api/eventos) y ajustar los ids de las peticiones de respaldo. En un mismo perfil, dos pestañas comparten la sesión del comprador.
- Abrir REST Client con los archivos [01](../http/01-salud.http), [07](../http/07-seguridad.http) y [13](../http/13-cancelacion-evento.http), la consola [Artemis](http://localhost:8161/console) (demo: `passly` / `passly`), el ADR de Facade, el tablero y el CI del mismo commit. Correr la suite completa antes de exponer. Dejar un solo terminal listo para el test corto y otro en la raíz para consultar la factura.
- En Artemis, verificar en el ensayo que se puede ejecutar `pause()` y `resume()` sobre la cola local `orden.pagada`, y que hay un consumidor activo. Ubicar `Operations` en el menú de la cola o en Artemis JMX, y `Browse` para ver el mensaje. Si funciona, pausar la cola justo antes de la compra. Si la consola no ofrece esas operaciones, dejar el consumidor activo, anotar `MessagesAdded` y `MessagesAcknowledged` y mostrar su incremento al final. [Operaciones de cola de Artemis](https://artemis.apache.org/components/artemis/documentation/latest/management.html)

| Cuenta de demo | Uso |
|---|---|
| `comprador@passly.test` | Carla compra una General para Festival de Jazz - Verano. |
| `validador@passly.test` | Vera valida en nombre de Aurora Producciones. |
| `organizador@passly.test` | Omar publica y cancela el evento de prueba de Aurora. |
| `admin@passly.test` | Ada abre el modelo de módulos. |

Contraseña de demo `passly1234`. Tarjeta de prueba `4242424242424242`, Carla Compradora, vencimiento `12/29`, código `123`.

## Recorrido en vivo

El reparto es una propuesta para confirmar en el ensayo. Cada integrante explica al menos un tramo que programó otra persona.

| Minuto | Quién | Acción y evidencia |
|---|---|---|
| 0:00–1:30 | Fede · Capas | «Vamos a comprar una entrada desde un link, validarla en la puerta y seguir esa orden hasta Facturación». Mostrar `docker compose ps`, backend en perfil `demo` y web. En [01-salud.http](../http/01-salud.http), ejecutar Health, Login de admin y Modelo de módulos. Mostrar el resultado de `EstructuraDeModulosTest` y señalar `EventoController`, `EventoServiceImpl` y `EventoRepository`. |
| 1:30–5:00 | Lucio · Compra y puerta | Abrir el link de Jazz, entrar como Carla, sumar una General, tocar «Continuar al pago» y pagar. Anotar el **id de orden**. En el ticket, abrir «Alternativa para ingreso manual» y copiar el **contenido firmado completo**. En el otro perfil, Vera elige Jazz en `/validar`, pega el contenido y valida. Mostrar **Válido**. Pulsar «Validar siguiente entrada», pegar el mismo contenido y mostrar **Ya usado** con la hora original. |
| 5:00–6:30 | Simón · Seguridad | En [07-seguridad.http](../http/07-seguridad.http), ejecutar «Login de comprador» y señalar el campo `token`. Enviar «COMPRADOR autenticado» al crear un evento (**403**) y «Token adulterado» en `GET /api/usuarios/me` (**401**). «El 403 corresponde a Carla, que está identificada y carece del rol. El 401 corresponde a un token que el servidor rechaza». |
| 6:30–8:30 | Juan · State | En [13-cancelacion-evento.http](../http/13-cancelacion-evento.http), ejecutar el login de Omar y los bloques **1, 2, 2b, 4 y 5** en ese orden. Mostrar `BORRADOR → PUBLICADO`, **409** al republicar, **200** al cancelar y **409 ProblemDetail** al repetir. Abrir `Cancelado.cancelar()` y señalar el rechazo. El archivo crea y usa su propio evento de prueba. |
| 8:30–10:30 | Simón · Mensajería | En Artemis, abrir `orden.pagada` y mostrar el JSON con el **mismo id de orden**. Reanudar la cola y consultar la factura con el comando de abajo. Mostrar la fila en `PENDIENTE_AFIP`. Si la cola quedó activa, mostrar los contadores y la misma fila. «La venta ya se confirmó. Facturación tomó la orden de la cola y la dejó pendiente del paso fiscal». |
| 10:30–11:45 | Fede · Pruebas | Ejecutar el test corto de abajo y explicar la expectativa: si la pasarela rechaza, Ventas conserva el carrito y evita confirmar la orden. Mostrar el resultado de la suite y la cobertura JaCoCo del mismo commit en [GitHub Actions](https://github.com/SimonForteza/Passly/actions). |
| 11:45–12:45 | Juan · Patrones | Mostrar el [ADR-003 de Facade](../adr/ADR-003-facade-servicio-de-ventas.md) junto a `VentaService` y su implementación. Señalar el método que el controlador usa para confirmar una orden y explicar cómo coordina el cobro y la confirmación. Tener a mano el [ADR-006 de State](../adr/ADR-006-state-ciclo-de-vida-del-evento.md) para preguntas. |
| 12:45–14:00 | Lucio · Tablero y diseño | Mostrar los responsables y estados en el [tablero](https://linear.app/passly), y seguir una historia de la [matriz](../trazabilidad.md) hasta su issue. Abrir el [registro de feedback](../feedback-diseno.md) y mostrar una devolución con su cambio si quedó registrada. Si sigue pendiente la respuesta de la cátedra, indicar qué falta confirmar. |
| 14:00–15:00 | Equipo · Margen | Recuperar un paso que haya demorado o responder preguntas. |

Desde la raíz, sustituir `123` por el id recién generado:

```sh
docker compose exec -T postgres psql -U passly -d passly -c "SELECT orden_id, total, estado FROM facturacion.factura WHERE orden_id = 123;"
```

Desde `backend/`, tener listo el test corto. Correr `./mvnw test` durante el ensayo y guardar la salida:

```sh
./mvnw "-Dtest=VentaServiceImplTest#pagoRechazadoNoConfirmaNiVaciaElCarrito" test
```

## Si falla algo

- **Cámara o lector:** usar ingreso manual desde el inicio. El valor que se pega está en «Alternativa para ingreso manual», debajo del QR.
- **Web caída con backend disponible:** en [09-accesos.http](../http/09-accesos.http), ejecutar los logins de Carla y Vera, asegurar una sesión de carrito nueva, agregar una entrada, comprar, consultar el ticket y ejecutar la misma validación dos veces. Anotar el id de esta compra para Facturación.
- **Cola o factura:** si quedó pausada, reanudarla. Si tras 30 segundos no aparece la factura, guardar el id de orden, revisar el log y seguir con pruebas. Con tiempo en el ensayo, usar el [guion de mensajería](../mensajeria.md) para inspeccionar el mensaje con el consumidor detenido y verificar el consumo al reactivarlo.
- **Docker lento o sin internet:** dejar las imágenes, dependencias y servicios listos antes del turno. Tener una segunda computadora ensayada con el mismo commit si el equipo dispone de ella. Llevar localmente el PDF, la salida de pruebas y la evidencia de CI y del tablero. Indicar qué tramo se pudo ejecutar en vivo si falla el entorno.
