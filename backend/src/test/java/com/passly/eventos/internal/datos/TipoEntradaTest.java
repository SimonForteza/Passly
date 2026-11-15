package com.passly.eventos.internal.datos;

import com.passly.eventos.CupoInsuficienteException;
import com.passly.eventos.EdicionDeEventoInvalidaException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Invariantes de cupo de {@link TipoEntrada}, unitario puro. Los dos casos de "editar con ventas
 * previas" vivian en {@code EdicionDeEventoTest}, armados vendiendo sobre un borrador; desde
 * PAS-35 un borrador no vende, asi que la invariante se prueba aca, donde vive.
 */
class TipoEntradaTest {

    @Test
    void bajarElCupoTotalPorDebajoDeLoVendidoLanzaEdicionInvalidaYNoDejaCambios() {
        TipoEntrada tipo = new TipoEntrada("General", new BigDecimal("1000.00"), 5);
        tipo.descontar(3);

        assertThatThrownBy(() -> tipo.editar("General", new BigDecimal("1000.00"), 2))
                .isInstanceOf(EdicionDeEventoInvalidaException.class);
        assertThat(tipo.getCupoTotal()).isEqualTo(5);
        assertThat(tipo.getCupoDisponible()).isEqualTo(2);
    }

    @Test
    void subirElCupoTotalRecalculaElDisponibleContraLoVendido() {
        TipoEntrada tipo = new TipoEntrada("General", new BigDecimal("1000.00"), 5);
        tipo.descontar(3);

        tipo.editar("General VIP", new BigDecimal("1500.00"), 10);

        assertThat(tipo.getNombre()).isEqualTo("General VIP");
        assertThat(tipo.getPrecio()).isEqualByComparingTo("1500.00");
        assertThat(tipo.getCupoTotal()).isEqualTo(10);
        assertThat(tipo.getCupoDisponible()).isEqualTo(7);
    }

    @Test
    void descontarMasDeLoDisponibleLanzaCupoInsuficiente() {
        TipoEntrada tipo = new TipoEntrada("General", new BigDecimal("1000.00"), 2);
        assertThatThrownBy(() -> tipo.descontar(3)).isInstanceOf(CupoInsuficienteException.class);
        assertThat(tipo.getCupoDisponible()).isEqualTo(2);
    }
}
