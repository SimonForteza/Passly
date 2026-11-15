# ADR-004: Port y Adapter para los proveedores externos

- Estado: Aceptado
- Fecha: 2026-11-15
- Responsables: Equipo Passly
- Alcance: Componentes `ventas`, `pagos` y `facturacion`

## Contexto

Passly depende de dos proveedores externos con protocolos distintos: una pasarela de pago REST y AFIP, un servicio legado SOAP con WSDL. Ninguno se controla desde Passly: pueden cambiar de proveedor, de version o caerse.

Cuando se implemento Ventas (PAS-8) el componente de Pagos todavia no existia. Ventas necesitaba cobrar igual, y el dia que Pagos existiera tenia que poder usarlo sin reescribir la compra. Con Facturacion pasa lo mismo: el registro de facturas por cola existe (PAS-32), pero el cliente SOAP de AFIP queda para el 30/11.

## Fuerzas y restricciones

- La logica de negocio no tiene que conocer el protocolo (REST, SOAP) ni los tipos del proveedor.
- Cambiar de proveedor tiene que ser escribir otro adapter, no tocar la orquestacion.
- Hay que distinguir el rechazo de negocio (la pasarela dijo que no, 402) de la caida tecnica (no respondio, 503).
- Las llamadas externas quedan fuera de toda transaccion (ADR-003).
- Los datos de tarjeta no se persisten.

## Alternativas consideradas

### Llamar a `PagoService` directo desde `VentaServiceImpl`

Es menos codigo. Descartada: `VentaServiceImpl` quedaria atado a los DTO y excepciones de `pagos`, y no se habria podido implementar Ventas antes que Pagos. Ademas, el contrato publico de Ventas (`VentaService.confirmarCompra`) terminaria exponiendo un tipo de otro modulo.

### Usar el cliente HTTP o SOAP directamente en el negocio

Por ejemplo, un `RestClient` dentro de `VentaServiceImpl`. Descartada: mezcla protocolo con reglas de negocio y no se puede probar sin red.

### Port y Adapter

Una interfaz definida por quien la necesita (el Port), en su propio vocabulario, y una clase que la implementa traduciendo al proveedor concreto (el Adapter).

## Decision

### Pago (implementado)

```
VentaServiceImpl -> PasarelaDePago (Port) -> PasarelaDePagoReal (Adapter) -> PagoService -> RestClient -> pasarela REST
```

- `ventas.internal.negocio.PasarelaDePago` es el Port: `cobrar(importe, referencia, tarjeta)` y `revertir(idComprobante)`, con tipos propios de Ventas (`ventas.dto.DatosDeTarjetaDTO`, `ComprobanteDeCobroDTO`, `ventas.PagoRechazadoException`).
- `ventas.internal.negocio.PasarelaDePagoReal` es el Adapter: traduce el `DatosDeTarjetaDTO` de Ventas al de Pagos y la `pagos.PagoRechazadoException` a la de Ventas. Es la unica clase de Ventas que importa `com.passly.pagos`.
- `pagos.internal.negocio.PagoServiceImpl` es a su vez el adapter de Passly hacia el proveedor: habla HTTP real con `RestClient` y traduce los 5xx, timeouts y conexiones rechazadas a `PasarelaDePagoNoDisponibleException` (503). La URL sale de `passly.pagos.pasarela.base-url`. Hoy apunta al mock `pagos.internal.pasarelamock.MockPasarelaDePagoController`, que corre en el mismo proceso.

La prueba de que el Port sirve: hasta PAS-29 Ventas cobraba con `PasarelaDePagoSimulada`. Se reemplazo por `PasarelaDePagoReal` y se borro la simulada **sin tocar `ConfirmacionDeCompra` ni `VentaServiceImpl`**. El Port tambien lo usa `CompraInvitadoService`.

### Facturacion (Port listo, Adapter SOAP pendiente)

- `facturacion.internal.negocio.ClienteAfip` es el Port: `solicitarFacturacion(idOrden)`.
- `ClienteAfipPendiente` es el adapter de hoy: solo deja un log. No simula una respuesta de AFIP. La factura queda en estado `PENDIENTE_AFIP`.
- El adapter SOAP (Spring Web Services + JAXB, con Fault y XXE) se agrega para el 30/11 como otra implementacion de `ClienteAfip`, sin tocar `FacturacionServiceImpl`.

## Consecuencias positivas

- Ventas se implemento y probo antes de que Pagos existiera.
- Cambiar de pasarela es cambiar una propiedad o escribir otro adapter.
- El contrato publico de Ventas no filtra tipos de Pagos.
- REST y SOAP quedan detras de la misma idea, cada uno en su componente.
- El adapter se prueba aislado (`PasarelaDePagoRealTest`).

## Consecuencias negativas

- Hay mas tipos: dos `DatosDeTarjetaDTO` y dos `PagoRechazadoException` con el mismo nombre en modulos distintos. Hay que explicar por que no es duplicacion: cada uno es el contrato de su modulo.
- Hay dos niveles de adaptacion para el pago (Ventas -> Pagos -> proveedor). Es mas indireccion, pero cada nivel tiene una sola razon para cambiar.
- Mientras el adapter SOAP no exista, Facturacion no emite comprobantes reales.

## Evidencia en el repositorio

- `backend/src/main/java/com/passly/ventas/internal/negocio/PasarelaDePago.java` y `PasarelaDePagoReal.java`.
- `backend/src/main/java/com/passly/pagos/internal/negocio/PagoServiceImpl.java` y `pagos/internal/pasarelamock/MockPasarelaDePagoController.java`.
- `backend/src/main/java/com/passly/facturacion/internal/negocio/ClienteAfip.java` y `ClienteAfipPendiente.java`.
- `backend/src/test/java/com/passly/ventas/internal/negocio/PasarelaDePagoRealTest.java`.
- `docs/http/09-pagos.http`: 402 con tarjeta rechazada y 503 con pasarela caida.

## Criterio de revision

Revisar cuando se agregue el adapter SOAP de AFIP o un segundo proveedor de pago. Si aparecen dos adapters activos a la vez para el mismo Port, decidir como se elige el bean (perfil, propiedad o Strategy).
