# Passly

Plataforma de venta y validacion de entradas para eventos. Ver [CLAUDE.md](CLAUDE.md) para la
arquitectura completa, el stack y las decisiones de diseno.

## Estado actual

Passly es **multi-productora**: un comprador elige entre fiestas de distintos organizadores y
cada productora publica y gestiona solo las suyas.

Cuatro componentes implementados, cada uno con las tres capas (presentacion / negocio / datos)
separadas: `ServicioDeUsuarios`, `ServicioDeProductoras`, `ServicioDeEventos` y el modulo
`seguridad`. Spring Modulith verifica las fronteras en el build y las dependencias estan
**declaradas** modulo por modulo: el grafo es la cadena `eventos -> productoras -> usuarios`.

La identidad de quien opera la resuelve **Spring Security** (HTTP Basic, sesion STATELESS):
ningun endpoint depende de un header propio para saber quien esta operando. La autorizacion
por rol es declarativa con `@PreAuthorize` en los controllers (ver [CLAUDE.md](CLAUDE.md) 4.11).

## Levantar el entorno

Requisitos: Java 21, Maven (o el wrapper `./mvnw` incluido), Docker.

```bash
# 1. Base de datos (Postgres local, descartable)
docker compose down -v && docker compose up -d

# 2. Backend, con datos de demo
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

El `down -v` recrea el volumen, y hace falta la primera vez despues de un cambio de esquema:
`db/init/` solo corre en la inicializacion del volumen, y con `ddl-auto: update` Hibernate no
agrega una columna `not null` a una tabla que ya tiene filas. Despues, `up -d` alcanza.

Sin el perfil `demo`, la base arranca vacia. Sin variables de entorno, el backend se conecta
al Postgres de `docker-compose.yml` (ver [.env.example](.env.example) para apuntar a Supabase).

## Probar que anda

```bash
curl http://localhost:8080/actuator/health           # {"status":"UP"}
curl http://localhost:8080/api/productoras           # las dos productoras de demo
curl http://localhost:8080/api/eventos               # cartelera, cada evento con su organizador
curl "http://localhost:8080/api/eventos?productora=1" # solo las fiestas de Aurora
curl http://localhost:8080/actuator/modulith         # modelo de modulos detectado

# El corazon de la demo: Olga es de otra productora -> 403
curl -i -X POST http://localhost:8080/api/eventos/3/publicacion -u organizador2@passly.test:passly1234
# Omar es el dueno de Aurora -> 200
curl -i -X POST http://localhost:8080/api/eventos/3/publicacion -u organizador@passly.test:passly1234
```

Guion completo de demo como archivos `.http` listos para la extension REST Client de VS Code
en [docs/http/](docs/http/), incluido
[06-aislamiento.http](docs/http/06-aislamiento.http): una productora no toca las fiestas de
otra.

## Correr los tests

```bash
cd backend
./mvnw test
```

Incluye `EstructuraDeModulosTest`, que verifica las fronteras entre modulos
(`ApplicationModules.verify()`), afirma la forma del grafo de dependencias y genera la
documentacion de la arquitectura desde el codigo en `target/spring-modulith-docs/`.

Para ver que las fronteras estan realmente verificadas y no solo documentadas: comentar la
anotacion `@ApplicationModule` de cualquier `package-info.java` y volver a correr los tests.
Falla con `Module 'eventos' depends on ... Allowed targets: none`.

## Nota para Windows: certificados y timezone

- Si Maven falla resolviendo plugins con `PKIX path building failed`, es un antivirus haciendo
  inspeccion TLS (Norton, en este equipo) cuyo certificado raiz el JDK no reconoce por defecto
  aunque Windows si. Se soluciona importando ese certificado al truststore de Java.
- La JVM esta fijada a `-Duser.timezone=UTC` en `pom.xml` (ver el comentario ahi): en Windows,
  Java reporta el timezone del sistema como `America/Buenos_Aires`, un alias que la imagen
  oficial de Postgres no reconoce (solo tiene `America/Argentina/Buenos_Aires`), y la conexion
  JDBC fallaba con `FATAL: invalid value for parameter "TimeZone"`.
