/**
 * Datos de demostracion. Solo se activa con el perfil {@code demo}; sin el, la base arranca vacia.
 *
 * <p><b>Por que la siembra vive afuera de los componentes y no adentro de cada uno.</b> Hasta que
 * los eventos tuvieron dueño, cada modulo podia sembrar lo suyo con su propio servicio. Con la
 * cadena {@code eventos -> productoras -> usuarios} eso dejo de cerrar: para crear un evento de
 * Aurora hay que actuar como alguien de Aurora, y Eventos <b>no puede consultar Usuarios</b> — su
 * {@code package-info} lo prohibe y el build lo hace cumplir. Un seeder adentro de Eventos habria
 * necesitado exactamente la dependencia que el diseño existe para evitar.
 *
 * <p>La salida no fue relajar la frontera sino reconocer que los datos de demo <b>no son de ningun
 * componente</b>: son un cliente del sistema, igual que la app web. Por eso viven aca y usan
 * unicamente contratos publicos. Que alcancen para operar el sistema entero es, de paso, la prueba
 * de que esos contratos estan completos.
 *
 * <p>Como consecuencia desaparecio el problema de coordinar tres {@code ApplicationRunner} con
 * {@code @Order}: un solo runner ejecuta los tres pasos en secuencia y los ids generados fluyen por
 * variables locales, sin tener que volver a buscarlos por clave natural.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
                "usuarios", "usuarios :: dto",
                "productoras", "productoras :: dto",
                "eventos", "eventos :: dto"})
package com.passly.demo;
