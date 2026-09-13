package com.passly;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica las fronteras entre modulos y genera su documentacion desde el codigo.
 *
 * <p><b>Que demuestra hoy.</b> Con cinco modulos y dependencias declaradas, {@code verify()} dejo
 * de ser tautologico: comprueba que nadie acceda a los {@code internal} de otro, que no haya ciclos,
 * y — lo mas importante — que <b>cada modulo solo dependa de lo que su {@code package-info} declara
 * como permitido</b>. Eventos no puede importar Usuarios ni aunque alguien lo intente por descuido:
 * el build falla antes de llegar a una revision de codigo.
 *
 * <p><b>El grafo cambio, y el build fue quien lo dijo — dos veces.</b> Hasta que los eventos
 * tuvieron dueño, este archivo afirmaba que Eventos era la raiz y no dependia de nadie. Cuando
 * aparecio el requerimiento de multiples productoras, esa prueba fallo — no una revision, no un
 * comentario en un PR: el build. Recien despues de declarar la dependencia en
 * {@code eventos/package-info.java} volvio a verde.
 *
 * <p>La segunda vez fue distinta, y vale decirlo en el oral: al agregar {@code ventas}, el build
 * <b>no</b> fallo — sus dependencias hacia {@code eventos} y {@code usuarios} ya estaban
 * declaradas desde el primer commit del modulo. Lo que se rompio fue la <i>narrativa</i>: este
 * archivo seguia afirmando que el grafo era una cadena de tres eslabones cuando ya habia pasado a
 * ser un grafo aciclico con {@code ventas} en la cima y dos caminos hacia Usuarios (uno directo,
 * otro via Eventos-Productoras). Nadie lo detecto por una falla roja; lo detecto una revision de
 * este mismo archivo. Es la diferencia entre lo que Modulith impone (las aristas permitidas) y lo
 * que sigue dependiendo del criterio del equipo (que la documentacion describa el grafo real).
 *
 * <p>La raiz sigue siendo <b>Usuarios</b>, y su {@code package-info} lo declara con
 * {@code allowedDependencies = {}} — que en Modulith significa <i>ninguna</i>, no "sin
 * restricciones". Asi la afirmacion la sostienen dos mecanismos independientes: la anotacion, que la
 * impone, y {@link #usuariosEsLaRaizDelGrafoDeDependencias()}, que la explicita.
 *
 * <p>Lo que Modulith <b>no</b> verifica: nada sobre las capas dentro de un modulo (que presentacion
 * no llame a datos, que la implementacion sea package-private). Solo vigila las fronteras
 * <i>entre</i> modulos.
 */
class EstructuraDeModulosTest {

    private static final ApplicationModules MODULOS = ApplicationModules.of(PasslyApplication.class);

    @Test
    void noHayViolacionesDeFrontera() {
        MODULOS.verify();
    }

    /**
     * La identidad es lo unico que el resto del sistema necesita sin tener nada que pedir a cambio.
     * Si Usuarios llegara a depender de otro componente, seria senal de que ese componente esta mal
     * ubicado.
     */
    @Test
    void usuariosEsLaRaizDelGrafoDeDependencias() {
        var usuarios = MODULOS.getModuleByName("usuarios").orElseThrow();
        assertThat(usuarios.getAllDependencies(MODULOS).isEmpty()).isTrue();
    }

    /**
     * El grafo de negocio ya no es una cadena: es un grafo aciclico con {@code ventas} en la
     * cima, que llega a Usuarios por <b>dos caminos</b> — directo, y via
     * {@code eventos -> productoras -> usuarios}.
     *
     * <p>Que ninguno de estos modulos toque los {@code internal} de otro, o importe algo que su
     * {@code package-info} no declare, no hace falta afirmarlo aca: lo garantiza
     * {@code allowedDependencies}, que {@link #noHayViolacionesDeFrontera()} hace cumplir. Este
     * test documenta la <i>forma</i> del grafo; aquel impide que se deforme sin que alguien lo
     * decida por escrito.
     */
    @Test
    void ventasEsLaCimaDelGrafoYUsuariosLaRaiz() {
        var usuarios = MODULOS.getModuleByName("usuarios").orElseThrow();
        var productoras = MODULOS.getModuleByName("productoras").orElseThrow();
        var eventos = MODULOS.getModuleByName("eventos").orElseThrow();
        var ventas = MODULOS.getModuleByName("ventas").orElseThrow();

        assertThat(productoras.getAllDependencies(MODULOS).contains(usuarios)).isTrue();
        assertThat(eventos.getAllDependencies(MODULOS).contains(productoras)).isTrue();
        assertThat(ventas.getAllDependencies(MODULOS).contains(eventos)).isTrue();
        assertThat(ventas.getAllDependencies(MODULOS).contains(usuarios)).isTrue();
    }

    @Test
    void generarDocumentacion() {
        new Documenter(MODULOS).writeDocumentation();
    }
}
