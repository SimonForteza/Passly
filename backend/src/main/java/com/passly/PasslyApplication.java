package com.passly;

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
        SpringApplication.run(PasslyApplication.class, args);
    }

}
