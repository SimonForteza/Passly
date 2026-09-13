package com.passly.ventas.internal.negocio;

import com.passly.ventas.PagoRechazadoException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasarelaDePagoSimuladaTest {

    private final PasarelaDePagoSimulada pasarela = new PasarelaDePagoSimulada();

    @Test
    void unImporteDentroDelLimiteSeAprueba() {
        ComprobanteDeCobroDTO comprobante = pasarela.cobrar(new BigDecimal("15000.00"));

        assertThat(comprobante.id()).startsWith("SIM-");
        assertThat(comprobante.importe()).isEqualByComparingTo("15000.00");
    }

    @Test
    void unImporteQueSuperaElLimiteSeRechaza() {
        BigDecimal excesivo = PasarelaDePagoSimulada.LIMITE.add(new BigDecimal("0.01"));

        assertThatThrownBy(() -> pasarela.cobrar(excesivo))
                .isInstanceOf(PagoRechazadoException.class);
    }

    @Test
    void revertirNoLanza() {
        ComprobanteDeCobroDTO comprobante = pasarela.cobrar(new BigDecimal("1000.00"));

        pasarela.revertir(comprobante.id());
    }
}
