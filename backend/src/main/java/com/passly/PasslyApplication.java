package com.passly;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de Passly.
 *
 * <p>El paquete de esta clase ({@code com.passly}) es la base del modelo de modulos de
 * Spring Modulith: cada sub-paquete directo — hoy solo {@code eventos} — es un componente
 * de negocio con su propia frontera, verificada en el build por
 * {@code EstructuraDeModulosTest}.
 */
@SpringBootApplication
public class PasslyApplication {

    public static void main(String[] args) {
        // El driver de Postgres manda el timezone por defecto de la JVM en el startup packet.
        // En Windows, TimeZone.getDefault() puede resolver al alias legado "America/Buenos_Aires",
        // que el Postgres del contenedor rechaza (FATAL: invalid value for parameter "TimeZone").
        // Se fuerza el nombre IANA canonico para que la conexion no dependa del SO de cada dev.
        TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"));
        SpringApplication.run(PasslyApplication.class, args);
    }

}
