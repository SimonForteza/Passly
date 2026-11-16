# Guion de la demo del 16/11

**Duración:** 15 minutos. **Turno:** confirmar el número de grupo y la hora en el aviso de Teams. Llegar antes con la computadora conectada al proyector.

## Preparación y ensayo del 15/11

1. Actualizar una copia de `main` y anotar el commit con `git rev-parse --short HEAD`. Reservar una instancia local descartable y comprobar qué contenedores pertenecen a esa copia con `docker compose ps -a`. Para el ensayo desde cero, correr `docker compose down -v` únicamente si todos los volúmenes de ese proyecto pertenecen a la demo y son descartables. En una instalación nueva alcanza con `docker compose up -d`.
2. En la raíz, correr `docker compose up -d --wait`. Abrir la consola de [Artemis](http://localhost:8161/console), usuario y clave de demo `passly`, y entrar a la cola `orden.pagada`. Dejar el consumidor de Facturación activo para que el mismo pago de la demo produzca la factura.
3. En otra terminal, desde `backend/`, correr `./mvnw spring-boot:run -Dspring-boot.run.profiles=demo` (`./mvnw.cmd` en Windows). Confirmar `UP` en [health](http://localhost:8080/actuator/health).
4. Desde `frontend/`, correr `npm ci` y `npm run dev`. Abrir [Passly](http://localhost:5173/). Dejar dos perfiles de navegador independientes para la compradora y la validadora. Reservar VS Code con REST Client para las peticiones.
5. Los cuatro recorren la demo el 15/11 por la noche. Cada integrante explica una parte que programó otra persona. Anotar el commit, asistentes y resultado en el [plan de entregas](../plan-entregas.md). Si falla un paso, registrar el issue o el defecto en `docs/defectos.md` con los pasos y la evidencia.

| Usuario del perfil demo | Acción |
|---|---|
| `comprador@passly.test` | Carla compra una entrada General para Festival de Jazz - Verano. |
| `validador@passly.test` | Vera valida entradas de Aurora Producciones. |
| `organizador@passly.test` | Omar opera los eventos de Aurora. |
| `admin@passly.test` | Ada puede consultar el modelo de Spring Modulith. |

Contraseña de las cuentas de prueba: `passly1234`. Tarjeta de la pasarela mock: `4242424242424242`, Carla Compradora, `12/29`, `123`.

## Recorrido en vivo

| Minutos | Mostrar | Acción y resultado esperado |
|---|---|---|
| 0:00–1:30 | Capas y componentes | Mostrar `docker compose ps`, backend con perfil `demo` y frontend. En [01-salud.http](../http/01-salud.http) ejecutar Health, login y Modulith. Abrir `EventoController`, `EventoServiceImpl` y `EventoRepository` para seguir presentación, negocio y datos. Mostrar el resultado de `./mvnw -Dtest=EstructuraDeModulosTest test`. |
| 1:30–5:00 | Compra y QR | Abrir [el evento de Jazz](http://localhost:5173/eventos/1). Si la base no es nueva, obtener el id en `GET /api/eventos`. Entrar como Carla, seleccionar una General, pagar con la tarjeta de prueba y anotar el número de orden. Abrir el QR. En el celular, abrir la app **Passly Validador** ([app-validador/](../../app-validador/README.md)), entrar como Vera y elegir Jazz. Escanear el QR que se ve en la notebook dos veces. Alternativa: en el segundo perfil, entrar como Vera, abrir [Validar](http://localhost:5173/validar) y elegir Jazz. La primera muestra **VÁLIDO**. Pulsar «Validar siguiente entrada» y repetir para ver **YA USADO** con la hora original. |
| 5:00–6:30 | JWT y autorización | En [07-seguridad.http](../http/07-seguridad.http), ejecutar «Login de comprador» y mostrar el campo token. Enviar «COMPRADOR autenticado» al crear evento (**403**) y «Token adulterado» en `GET /api/usuarios/me` (**401**). |
| 6:30–8:30 | State y conflicto | En [03-flujo-feliz.http](../http/03-flujo-feliz.http), iniciar sesión, crear y publicar el evento de prueba y repetir **Publicación** (**409**). Luego enviar `POST /api/eventos/{id}/cancelacion` con el JWT de Omar dos veces (**200**, **409**). Usar el id devuelto por «Crear» para conservar Jazz disponible. Mostrar la clase del estado que rechaza la transición. La cancelación y las clases dependen de [PAS-35](https://linear.app/passly/issue/PAS-35). |
| 8:30–10:30 | Mensajería | Volver a Artemis y mostrar los contadores de entrada y confirmación de la cola `orden.pagada`. Ejecutar la consulta de factura que figura abajo con la orden de Carla. Debe aparecer una fila en `PENDIENTE_AFIP`. Si se necesita inspeccionar el JSON en espera, hacer una segunda compra con el consumidor detenido y reiniciarlo siguiendo el [guion de mensajería](../mensajeria.md). |
| 10:30–12:00 | Pruebas | Desde `backend/`, ejecutar `./mvnw test`. Correr la suite antes del turno para poder mostrar el resumen e identificar una prueba unitaria. Abrir la [ejecución de CI](https://github.com/SimonForteza/Passly/actions) del mismo commit. La suite y el reporte de cobertura en CI dependen de [PAS-38](https://linear.app/passly/issue/PAS-38). |
| 12:00–13:30 | Patrones y ADR | Abrir [los ADR](../adr/) y ubicar el ADR de Facade de [PAS-36](https://linear.app/passly/issue/PAS-36) junto a `VentaService`. Explicar cómo la confirmación orquesta cobro, orden y tickets desde un punto de entrada. Tener a mano el ADR-001 para la decisión de arquitectura del monolito modular. |
| 13:30–15:00 | Tablero y feedback | Mostrar el tablero acordado con la cátedra, sus responsables, la [trazabilidad de historias](../trazabilidad.md) y las observaciones de [feedback de diseño](../feedback-diseno.md) con su evidencia. Confirmar este tramo en [PAS-39](https://linear.app/passly/issue/PAS-39). |

La consulta usa el id numérico de la compra. Reemplazar `123` por el número de orden mostrado en pantalla.

```sh
docker compose exec -T postgres psql -U passly -d passly -c "SELECT * FROM facturacion.factura WHERE orden_id = 123;"
```

### Plan B

- **Cámara:** en el ticket abrir «Alternativa para ingreso manual». Pegar el contenido firmado completo en «Ingreso manual del QR» dentro de `/validar` y validar dos veces. El código corto que aparece bajo el QR se usa como referencia visual.
- **Docker lento:** descargar las imágenes con `docker compose pull` y levantar los servicios antes del turno. Mantener el entorno preparado entre el ensayo y la exposición. Si la web sigue esperando, usar [09-accesos.http](../http/09-accesos.http) para mostrar el flujo con el backend operativo. Usar una compra nueva para evitar un QR consumido.
- **Sin internet:** precargar dependencias Maven y npm, las imágenes Docker y el PDF. El backend, la pasarela mock, la web y Artemis se ejecutan localmente. Llevar el resultado del CI y la referencia del tablero al mismo commit. Informar si la red impide abrir GitHub o el tablero durante el turno.
