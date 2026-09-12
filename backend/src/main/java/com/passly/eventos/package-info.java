/**
 * <b>ServicioDeEventos</b>: alta, publicacion y consulta de eventos, sus tipos de entrada y sus
 * cupos.
 *
 * <p><b>Eventos dejo de ser la raiz del grafo.</b> Hasta que los eventos tuvieron dueno, este modulo
 * no dependia de nadie y {@code EstructuraDeModulosTest} lo verificaba. Cuando aparecio el
 * requerimiento de multiples productoras eso cambio, y lo importante es <b>como</b> cambio: el build
 * fallo con {@code Module 'eventos' depends on ... Allowed targets: none} hasta que la dependencia
 * quedo declarada aca por escrito. No hubo forma de que el acoplamiento entrara sin que alguien lo
 * decidiera.
 *
 * <p>La superficie de contacto es chica y esta acotada a proposito: Eventos le pregunta a Productoras
 * quien puede gestionar sus eventos y como se llama comercialmente. <b>No depende de Usuarios</b> —
 * ni lo necesita: la cadena {@code eventos -> productoras -> usuarios} deja la identidad detras de
 * Productoras, que ya resolvio el cruce de roles al armar su padron. Si este modulo importara
 * {@code com.passly.usuarios}, el build fallaria.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"productoras", "productoras :: dto"})
package com.passly.eventos;
