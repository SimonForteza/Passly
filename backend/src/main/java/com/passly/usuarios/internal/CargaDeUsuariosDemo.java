package com.passly.usuarios.internal;

import com.passly.usuarios.Rol;
import com.passly.usuarios.UsuarioService;
import com.passly.usuarios.dto.CrearUsuarioRequest;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Siembra usuarios de demo, activa solo con el perfil {@code demo}
 * ({@code -Dspring-boot.run.profiles=demo}). Sin el perfil, la base arranca vacia.
 *
 * <p>Siembra <b>a traves de {@link UsuarioService}</b> y no con SQL crudo: los datos de prueba
 * pasan por las mismas validaciones y el mismo hashing que produccion, asi que si el seeder
 * arranca sin excepciones, el alta funciona — el seed es en si mismo un smoke test. Es
 * idempotente: si ya hay usuarios, no hace nada.
 *
 * <p>El nombre lleva sufijo a proposito: {@code eventos} ya tiene un {@code CargaDeDatosDemo} y
 * dos beans con el mismo nombre simple rompen el arranque.
 *
 * <p><b>Corre primero de los tres seeders</b> ({@code @Order(10)}), porque Productoras necesita
 * usuarios existentes para armar su padron y Eventos necesita productoras. Los ids son
 * {@code IDENTITY} y por lo tanto impredecibles, asi que los seeders siguientes resuelven lo que
 * necesitan por clave natural — el email — y no por id.
 *
 * <p>Se usa {@code @Order} y no {@code @DependsOn}: {@code @DependsOn} ordena la <i>creacion de
 * beans</i>, que aca es irrelevante porque los tres se crean antes de que corra ninguno. Lo que hay
 * que ordenar es la <i>ejecucion</i> de los {@code ApplicationRunner}, y de eso se encarga
 * {@code Ordered}.
 */
@Component
@Profile("demo")
@Order(10)
class CargaDeUsuariosDemo implements ApplicationRunner {

    /** Password comun para todos los usuarios de demo. Cumple el minimo de 8 caracteres. */
    private static final String PASSWORD_DEMO = "passly1234";

    private final UsuarioService usuarioService;

    CargaDeUsuariosDemo(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!usuarioService.listarUsuarios().isEmpty()) {
            return;
        }

        // Un usuario por rol, para tener con que probar la seguridad por rol en PAS-6.
        usuarioService.registrarUsuario(new CrearUsuarioRequest(
                "comprador@passly.test", "Carla Compradora", PASSWORD_DEMO, Rol.COMPRADOR));
        usuarioService.registrarUsuario(new CrearUsuarioRequest(
                "organizador@passly.test", "Omar Organizador", PASSWORD_DEMO, Rol.ORGANIZADOR));
        usuarioService.registrarUsuario(new CrearUsuarioRequest(
                "validador@passly.test", "Vera Validadora", PASSWORD_DEMO, Rol.VALIDADOR));
        usuarioService.registrarUsuario(new CrearUsuarioRequest(
                "admin@passly.test", "Ada Admin", PASSWORD_DEMO, Rol.ADMIN));

        // Un segundo organizador, de otra productora. Sin el no hay marketplace que mostrar: con
        // un solo organizador no se puede demostrar que una productora no toca los eventos de otra.
        usuarioService.registrarUsuario(new CrearUsuarioRequest(
                "organizador2@passly.test", "Olga Organizadora", PASSWORD_DEMO, Rol.ORGANIZADOR));
    }
}
