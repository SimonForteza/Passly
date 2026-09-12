package com.passly;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica las fronteras entre modulos y genera su documentacion desde el codigo.
 *
 * <p><b>Que demuestra hoy.</b> Con cuatro modulos y dependencias declaradas, {@code verify()} dejo
 * de ser tautologico: comprueba que nadie acceda a los {@code internal} de otro, que no haya ciclos,
 * y — lo mas importante — que <b>cada modulo solo dependa de lo que su {@code package-info} declara
 * como permitido</b>. Eventos no puede importar Usuarios ni aunque alguien lo intente por descuido:
 * el build falla antes de llegar a una revision de codigo.
 *
 * <p><b>El grafo cambio, y el build fue quien lo dijo.</b> Hasta que los eventos tuvieron dueño,
 * este archivo afirmaba que Eventos era la raiz y no dependia de nadie. Cuando aparecio el
 * requerimiento de multiples productoras, esta prueba fallo — no una revision, no un comentario en
 * un PR: el build. Recien despues de declarar la dependencia en
 * {@code eventos/package-info.java} volvio a verde. Esa es la diferencia entre una frontera
 * documentada y una verificada, y es reproducible: comentar la anotacion de cualquier
 * {@code package-info} y correr {@code ./mvnw test} vuelve a mostrarlo.
 *
 * <p>La raiz ahora es <b>Usuarios</b>, y su {@code package-info} lo declara con
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
     * El grafo de negocio es la cadena {@code eventos -> productoras -> usuarios}.
     *
     * <p>Que Eventos no toque Usuarios <b>directamente</b> no hace falta afirmarlo aca: lo garantiza
     * {@code allowedDependencies} en su {@code package-info}, que {@link #noHayViolacionesDeFrontera()}
     * hace cumplir. Este test documenta la forma del grafo; aquel impide que se deforme.
     */
    @Test
    void elGrafoDeNegocioEsUnaCadena() {
        var usuarios = MODULOS.getModuleByName("usuarios").orElseThrow();
        var productoras = MODULOS.getModuleByName("productoras").orElseThrow();
        var eventos = MODULOS.getModuleByName("eventos").orElseThrow();

        assertThat(productoras.getAllDependencies(MODULOS).contains(usuarios)).isTrue();
        assertThat(eventos.getAllDependencies(MODULOS).contains(productoras)).isTrue();
    }

    @Test
    void generarDocumentacion() {
        new Documenter(MODULOS).writeDocumentation();
    }
}
