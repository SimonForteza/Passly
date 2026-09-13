# Documentacion generada por Spring Modulith (PAS-9)

Todo lo que hay en este directorio, salvo este README, lo escribe `Documenter` a partir del
codigo (`package-info.java`, Javadoc de las interfaces publicas, y el grafo de dependencias
real). Nadie lo tipea a mano ni lo dibuja aparte: si el codigo cambia, se regenera y listo.
Es la misma logica que `docs/ddl-eventos.sql` aplica al esquema — documentacion de referencia
que otra herramienta ya genera, versionada igual porque hace defendible el oral.

## Como se genera

```bash
cd backend
./mvnw test -Dtest=EstructuraDeModulosTest
```

El metodo `generarDocumentacion()` de `EstructuraDeModulosTest`
(`backend/src/test/java/com/passly/EstructuraDeModulosTest.java`) llama a
`new Documenter(MODULOS).writeDocumentation()`, que escribe en
`backend/target/spring-modulith-docs/` (carpeta `target/`, ignorada por git). Los archivos de
este directorio son una copia de esa salida en el momento del commit — para actualizarlos
despues de un cambio real de dependencias entre modulos, correr el comando de arriba y volver
a copiar:

```bash
cp backend/target/spring-modulith-docs/{components.puml,module-*.puml,module-*.adoc,all-docs.adoc} \
   docs/modulith/
```

## Que hay en cada archivo

| Archivo | Que muestra |
|---|---|
| [`components.puml`](components.puml) | El diagrama global: los 6 modulos que hoy tienen alguna arista de dependencia con otro (Usuarios, Productoras, Eventos, Ventas, Seguridad, Demo) y el sentido de cada flecha ("uses") |
| `module-<nombre>.puml` | Un diagrama por modulo (los 7: suma Pagos, que en `components.puml` no aparece — ver abajo) |
| `module-<nombre>.adoc` | Descripcion en prosa de cada modulo: paquete base, Spring beans expuestos, y el Javadoc real de la interfaz publica y del `package-info` (por eso cita cosas como "CLAUDE.md 4.3, regla 1" tal cual estan escritas en el codigo) |
| [`all-docs.adoc`](all-docs.adoc) | Agrega todo lo anterior en un solo documento AsciiDoc (`include::` a cada `module-*.adoc`) |

**Por que `Pagos` no aparece en `components.puml`.** No es un bug del diagrama: hoy ningun
modulo declara una dependencia hacia `pagos` en su `package-info`, y `pagos` tampoco depende de
nadie (`allowedDependencies = {}`) — es exactamente lo que CLAUDE.md 4.6 declara como pendiente:
`ventas` todavia cobra con `PasarelaDePagoSimulada`, no con el `ServicioDePagos` real. Un modulo
sin ninguna arista no tiene nada que dibujar en la vista global, pero sigue teniendo su propio
`module-pagos.puml` (una caja sola, sin flechas) y su `module-pagos.adoc`. El dia que se cablee
`VentaServiceImpl` a `PagoService`, regenerar esto va a hacer aparecer la arista
`Ventas -> Pagos` sin que nadie tenga que actualizar un diagrama a mano.

## Como renderizar los `.puml`

Esta maquina no tiene `plantuml` ni `graphviz` instalados, asi que estos archivos quedan como
*fuente* versionada, no como imagen. Para verlos:

- VS Code: extension `jebbs.plantuml` (o cualquier extension de PlantUML), abrir el archivo y
  `Alt+D`.
- Sin instalar nada: pegar el contenido en <https://www.plantuml.com/plantuml/uml/>.
- Con `plantuml` instalado: `plantuml docs/modulith/components.puml` genera un `.png` al lado.

## La prueba en vivo: romper una regla a proposito

`docs/http/README.md` ya describe este ejercicio ("Demostrar que las fronteras estan
verificadas"); esto es la corrida real, hecha para cerrar el criterio de aceptacion de PAS-9.

Se cambio momentaneamente `eventos/package-info.java`
(`backend/src/main/java/com/passly/eventos/package-info.java`) de

```java
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"productoras", "productoras :: dto"})
```

a

```java
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {})
```

y se corrio `cd backend && ./mvnw test -Dtest=EstructuraDeModulosTest`. Paso de 4/4 en verde a
esto:

```
[ERROR] com.passly.EstructuraDeModulosTest.noHayViolacionesDeFrontera -- Time elapsed: 0.065 s <<< ERROR!
org.springframework.modulith.core.Violations:
- Module 'eventos' depends on module 'productoras' via com.passly.eventos.internal.negocio.EventoServiceImpl -> com.passly.productoras.ProductoraService. Allowed targets: none.
- Module 'eventos' depends on named interface(s) 'productoras :: dto' via com.passly.eventos.internal.negocio.EventoServiceImpl -> com.passly.productoras.dto.ProductoraDTO. Allowed targets: none.
```

Dos violaciones, no una: la dependencia al contrato (`ProductoraService`) y la dependencia al
`@NamedInterface("dto")` (`ProductoraDTO`), porque `EventoServiceImpl` usa ambas. Se revirtio el
archivo (`git checkout -- backend/src/main/java/com/passly/eventos/package-info.java`) y
`EstructuraDeModulosTest` volvio a 4/4 en verde. El cambio de este ejercicio no quedo
commiteado — es evidencia de que el test reacciona, no parte del codigo final.

## CI

`.github/workflows/modulith-boundaries.yml` corre este mismo test en cada push/PR. No necesita
Postgres/Docker: `ApplicationModules.verify()` solo introspecciona el bytecode compilado.
