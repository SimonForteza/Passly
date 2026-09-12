# ADR-002: Esquema PostgreSQL por componente

- Estado: Aceptado
- Fecha: 2026-09-11
- Ultima revision: 2026-09-12
- Responsables: Equipo Passly
- Alcance: Persistencia del backend

## Contexto

Los componentes de Passly son responsables de su propio modelo de dominio. Eventos administra eventos, tipos de entrada, precios y cupos; Productoras administra organizadores, miembros y roles internos; Usuarios administra identidades, roles globales y credenciales. Los componentes futuros tendran modelos propios para ventas, pagos, tickets, accesos, notificaciones y facturacion.

La persistencia debe reflejar esos limites sin agregar una carga operativa desproporcionada para un monolito modular y un equipo pequeno. Tambien debe evitar que un componente dependa directamente de tablas o entidades pertenecientes a otro.

## Fuerzas y restricciones

- PostgreSQL es la tecnologia de persistencia elegida.
- El desarrollo local debe funcionar con un unico contenedor.
- Las transacciones multipaso deben poder ejecutarse localmente.
- Cada componente debe ser propietario de sus datos.
- Las consultas entre componentes deben pasar por contratos de negocio.
- El diseño debe ser compatible con Supabase sin acoplar el dominio a su API REST.

## Alternativas consideradas

### Esquema compartido

Todos los componentes utilizan las mismas tablas o un mismo espacio de nombres. Simplifica la configuracion inicial, pero facilita joins, claves foraneas y accesos directos que rompen la autonomia de los componentes.

### Base de datos por componente

Cada componente utiliza una instancia o base independiente. Ofrece aislamiento fisico y autonomia operativa, pero agrega conexiones, migraciones, backups y observabilidad separada. Tambien impide transacciones ACID simples entre componentes y acerca el sistema a una arquitectura distribuida que no fue elegida.

### Esquema por componente

Una unica instancia PostgreSQL contiene un esquema separado por componente. Cada modulo mapea sus entidades exclusivamente a su esquema y accede a otros datos mediante interfaces de negocio.

## Decision

Utilizar **una instancia PostgreSQL con un esquema por componente**.

Los esquemas se declaran inicialmente en `db/init/01-esquemas.sql`. Las entidades JPA indican el esquema al que pertenecen. Quedan prohibidos los joins, repositorios y claves foraneas que atraviesen esquemas de componentes distintos. Las referencias entre componentes se guardan como identificadores simples y se validan mediante el contrato publico del componente propietario.

Eventos guarda `productora_id`, pero consulta y autoriza a traves de `ProductoraService`; Productoras guarda `usuario_id`, pero valida la identidad mediante `UsuarioService`. Si Ventas necesita consultar disponibilidad o precio, debe invocar `EventoService`; no puede consultar las tablas de Eventos.

## Consecuencias positivas

- La propiedad de los datos queda visible y es facil de explicar.
- Un solo PostgreSQL simplifica desarrollo, despliegue, backup y demostracion.
- Las transacciones locales siguen disponibles.
- Los nombres de tablas no colisionan entre componentes.
- La separacion facilita una futura extraccion a bases independientes.
- El dominio no depende de PostgREST ni de detalles de Supabase.

## Consecuencias negativas

- El aislamiento es logico, no fisico.
- Una cuenta de base con permisos amplios podria acceder a todos los esquemas.
- Un fallo de la instancia afecta a todos los componentes.
- Las migraciones comparten una ventana de despliegue.
- PostgreSQL no impide por si solo los joins entre esquemas; la regla debe verificarse mediante diseño, revision y pruebas.

## Evidencia en el repositorio

- `db/init/01-esquemas.sql` crea los esquemas `eventos`, `productoras` y `usuarios`.
- Las entidades JPA de Eventos, Productoras y Usuarios especifican su esquema propietario.
- `eventos.evento.productora_id` y `productoras.miembro.usuario_id` son referencias logicas sin claves foraneas entre esquemas.
- Los servicios publicos intercambian DTO y no entidades.
- `docker-compose.yml` levanta una unica instancia PostgreSQL para desarrollo.
- La conexion puede cambiarse a Supabase mediante variables de entorno, sin modificar codigo de dominio.

## Evolucion prevista

Durante el desarrollo se utiliza `ddl-auto: update`. Antes de la Obligatoria 2 debe evaluarse Flyway o `ddl-auto: validate` para versionar los cambios de cada esquema de manera reproducible.

## Criterio de revision

Revisar esta decision si un componente se extrae a otro proceso, requiere aislamiento regulatorio o demuestra necesidades de disponibilidad o escalado incompatibles con la instancia compartida.
