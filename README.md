# Passly

Plataforma de venta y validacion de entradas para eventos. Ver [CLAUDE.md](CLAUDE.md) para la
arquitectura completa, el stack y las decisiones de diseno.

## Estado actual

`ServicioDeEventos` implementado: alta, publicacion y consulta de eventos y tipos de entrada,
con las tres capas (presentacion / negocio / datos) separadas y Spring Modulith verificando
las fronteras en el build.

## Levantar el entorno

Requisitos: Java 21, Maven (o el wrapper `./mvnw` incluido), Docker.

```bash
# 1. Base de datos (Postgres local, descartable)
docker compose up -d

# 2. Backend, con datos de demo
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

Sin el perfil `demo`, la base arranca vacia. Sin variables de entorno, el backend se conecta
al Postgres de `docker-compose.yml` (ver [.env.example](.env.example) para apuntar a Supabase).

## Probar que anda

```bash
curl http://localhost:8080/actuator/health          # {"status":"UP"}
curl http://localhost:8080/api/eventos              # cartelera publica
curl http://localhost:8080/actuator/modulith         # modelo de modulos detectado
```

Guion completo de demo (crear, publicar, el 409 al republicar, etc.) en el plan de
implementacion del componente y en `docs/`.

## Correr los tests

```bash
cd backend
./mvnw test
```

Incluye `EstructuraDeModulosTest`, que verifica las fronteras entre modulos
(`ApplicationModules.verify()`) y genera la documentacion de la arquitectura desde el codigo
en `target/spring-modulith-docs/`.

## Nota para Windows: certificados y timezone

- Si Maven falla resolviendo plugins con `PKIX path building failed`, es un antivirus haciendo
  inspeccion TLS (Norton, en este equipo) cuyo certificado raiz el JDK no reconoce por defecto
  aunque Windows si. Se soluciona importando ese certificado al truststore de Java.
- La JVM esta fijada a `-Duser.timezone=UTC` en `pom.xml` (ver el comentario ahi): en Windows,
  Java reporta el timezone del sistema como `America/Buenos_Aires`, un alias que la imagen
  oficial de Postgres no reconoce (solo tiene `America/Argentina/Buenos_Aires`), y la conexion
  JDBC fallaba con `FATAL: invalid value for parameter "TimeZone"`.
