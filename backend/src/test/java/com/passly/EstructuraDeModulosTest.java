package com.passly;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica las fronteras entre modulos y genera su documentacion desde el codigo.
 *
 * <p><b>Que demuestra hoy.</b> Con los modulos actuales y dependencias declaradas, {@code verify()} dejo
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
 * <p>Usuarios y Tickets son las dos raices actuales: no tienen dependencias salientes. Sus
 * {@code package-info} declaran {@code allowedDependencies = {}}, que en Modulith significa
 * <i>ninguna</i>, no "sin restricciones". Ventas depende de ambas y tambien de Eventos.
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
     * Usuarios custodia identidad y Tickets recibe snapshots completos al emitir. Ninguno necesita
     * consultar otro componente para cumplir su responsabilidad.
     */
    @Test
    void usuariosYTicketsSonRaicesDelGrafoDeDependencias() {
        var usuarios = MODULOS.getModuleByName("usuarios").orElseThrow();
        var tickets = MODULOS.getModuleByName("tickets").orElseThrow();
        assertThat(usuarios.getAllDependencies(MODULOS).isEmpty()).isTrue();
        assertThat(tickets.getAllDependencies(MODULOS).isEmpty()).isTrue();
    }

    /**
     * El grafo de negocio ya no es una cadena: es un grafo aciclico con {@code ventas} en la
     * cima, que llega a Usuarios por <b>dos caminos</b> — directo, y via
     * {@code eventos -> productoras -> usuarios} — y ademas depende de Tickets.
     *
     * <p>Que ninguno de estos modulos toque los {@code internal} de otro, o importe algo que su
     * {@code package-info} no declare, no hace falta afirmarlo aca: lo garantiza
     * {@code allowedDependencies}, que {@link #noHayViolacionesDeFrontera()} hace cumplir. Este
     * test documenta la <i>forma</i> del grafo; aquel impide que se deforme sin que alguien lo
     * decida por escrito.
     */
    @Test
    void ventasEsLaCimaDelGrafo() {
        var usuarios = MODULOS.getModuleByName("usuarios").orElseThrow();
        var productoras = MODULOS.getModuleByName("productoras").orElseThrow();
        var eventos = MODULOS.getModuleByName("eventos").orElseThrow();
        var ventas = MODULOS.getModuleByName("ventas").orElseThrow();
        var tickets = MODULOS.getModuleByName("tickets").orElseThrow();

        assertThat(productoras.getAllDependencies(MODULOS).contains(usuarios)).isTrue();
        assertThat(eventos.getAllDependencies(MODULOS).contains(productoras)).isTrue();
        assertThat(ventas.getAllDependencies(MODULOS).contains(eventos)).isTrue();
        assertThat(ventas.getAllDependencies(MODULOS).contains(usuarios)).isTrue();
        assertThat(ventas.getAllDependencies(MODULOS).contains(tickets)).isTrue();
    }

    @Test
    void generarDocumentacion() {
        new Documenter(MODULOS).writeDocumentation();
    }
}
