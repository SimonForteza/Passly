package com.passly;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica las fronteras entre modulos y genera su documentacion desde el codigo.
 *
 * <p><b>Que demuestra hoy, con un solo modulo implementado:</b> {@code verify()} es casi
 * tautologico — comprueba que {@code eventos} es reconocido como modulo, que no hay ciclos
 * (imposible con uno) y que nadie de afuera accede a {@code internal}, trivial porque no hay
 * nadie afuera todavia. Su valor real aparece con el segundo componente: el andamiaje ya va a
 * estar puesto, y el segundo modulo nace con la red debajo en vez de escribirse primero y
 * auditarse despues (CLAUDE.md 4.4).
 *
 * <p>Lo que <b>si</b> es una afirmacion no trivial hoy es
 * {@link #eventosEsLaRaizDelGrafoDeDependencias()}: convierte en algo que el build verifica la
 * afirmacion del informe de que Eventos no depende de ningun otro componente (CLAUDE.md 4.6).
 *
 * <p>Lo que Modulith <b>no</b> verifica: nada sobre las capas dentro de un modulo (que
 * presentacion no llame a datos, que la implementacion sea package-private). Solo vigila las
 * fronteras <i>entre</i> modulos.
 */
class EstructuraDeModulosTest {

    private static final ApplicationModules MODULOS = ApplicationModules.of(PasslyApplication.class);

    @Test
    void noHayViolacionesDeFrontera() {
        MODULOS.verify();
    }

    @Test
    void eventosEsLaRaizDelGrafoDeDependencias() {
        var eventos = MODULOS.getModuleByName("eventos").orElseThrow();
        assertThat(eventos.getAllDependencies(MODULOS).isEmpty()).isTrue();
    }

    @Test
    void generarDocumentacion() {
        new Documenter(MODULOS).writeDocumentation();
    }
}
