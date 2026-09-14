# ADR-001: Monolito modular en lugar de microservicios

- Estado: Aceptado
- Fecha: 2026-09-11
- Ultima revision: 2026-09-14
- Responsables: Equipo Passly
- Alcance: Backend de Passly

## Contexto

Passly debe resolver un flujo de venta y validacion de entradas que atraviesa varios componentes: usuarios, eventos, ventas, pagos, tickets, accesos, notificaciones y facturacion. La consigna academica exige interfaces explicitas, transacciones declarativas, integraciones sincronicas y asincronicas, seguridad por rol y una demostracion en vivo.

El equipo necesita fronteras claras entre componentes, pero dispone de un equipo pequeno, un cronograma corto y un unico producto. En el corte actual, Ventas cobra mediante una pasarela simulada antes de abrir la transaccion local; `ConfirmacionDeCompra` coordina dentro de esa transaccion el descuento de cupo y la persistencia de la orden. La emision de tickets y el cableado con el Adapter real de Pagos siguen pendientes y no se presentan como atomicos ni implementados.

## Fuerzas y restricciones

- El sistema debe poder levantarse y demostrarse como una unidad.
- Las fronteras entre componentes no deben depender solo de disciplina humana.
- El flujo critico necesita transacciones locales y declarativas.
- El equipo debe minimizar complejidad operativa y tiempo de diagnostico.
- La arquitectura debe permitir explicar responsabilidades y dependencias durante la defensa oral.
- Los componentes deben poder evolucionar con bajo acoplamiento.

## Alternativas consideradas

### Monolito tradicional

Un unico despliegue y una unica base, sin mecanismos que verifiquen las fronteras internas. Es simple de operar, pero permite que cualquier clase acceda a cualquier entidad o repositorio. La separacion quedaria como una convencion fragil.

### Microservicios

Un proceso desplegable por componente, comunicacion por red y persistencia independiente. Brinda autonomia de despliegue y escalado, pero introduce fallos de red, observabilidad distribuida, contratos remotos, despliegues coordinados y consistencia eventual. El flujo transaccional de compra requeriria una saga o compensaciones.

### Monolito modular

Un unico artefacto ejecutable dividido en modulos de negocio con contratos publicos, implementaciones internas y dependencias verificadas durante el build.

## Decision

Implementar Passly como un **monolito modular** sobre Spring Boot y Spring Modulith.

Cada componente se ubica bajo `com.passly.<componente>`. Su interfaz, DTO y excepciones de contrato son publicos; las implementaciones, controladores, entidades y repositorios viven bajo `internal`. Los componentes se comunican mediante interfaces de negocio y DTO, nunca importando internos ajenos.

Spring Modulith verifica que no existan accesos indebidos ni ciclos entre modulos mediante `ApplicationModules.verify()`. El sistema se empaqueta y despliega como una sola aplicacion.

## Consecuencias positivas

- Las transacciones criticas permanecen locales y pueden implementarse con `@Transactional`.
- Existe un unico proceso para desarrollar, probar y demostrar.
- Las fronteras se verifican automaticamente en el build.
- Se reduce la complejidad de infraestructura, red y observabilidad.
- El codigo mantiene contratos que permitirian extraer componentes en el futuro.
- Los diagramas de modulos pueden generarse desde el codigo con Spring Modulith.

## Consecuencias negativas

- Todos los componentes se despliegan juntos.
- No existe escalado independiente por componente.
- Un problema grave del proceso puede afectar a toda la aplicacion.
- La disciplina de modularizacion debe sostenerse con tests y revisiones.
- Las entidades y repositorios pueden ser `public` por restricciones de paquetes Java, por lo que Spring Modulith sigue siendo necesario para proteger la frontera real.

## Evidencia en el repositorio

- `backend/src/test/java/com/passly/EstructuraDeModulosTest.java` verifica las fronteras.
- Usuarios, Productoras, Eventos, Seguridad, Ventas y Pagos exponen contratos y encapsulan sus implementaciones bajo `internal`.
- Los DTO compartibles se publican mediante `@NamedInterface("dto")`.
- Cada `package-info.java` declara sus dependencias permitidas y `EstructuraDeModulosTest` comprueba el grafo aciclico actual, con Ventas en la cima y Usuarios como raiz.
- El build genera documentacion de modulos y el workflow `modulith-boundaries.yml` ejecuta la verificacion de fronteras en GitHub Actions.

## Criterio de revision

Revisar esta decision si el sistema necesita despliegues independientes, escalado desigual demostrado con mediciones o limites organizacionales que justifiquen separar equipos y procesos. La popularidad de microservicios, por si sola, no constituye un motivo de cambio.
