package com.passly.ventas.internal.negocio;

import com.passly.ventas.CarritoDeOtroCompradorException;
import com.passly.ventas.CarritoVacioException;
import com.passly.ventas.CarritoVencidoException;
import com.passly.ventas.dto.CarritoDTO;
import com.passly.ventas.dto.ItemDeCarritoDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba unitaria pura de {@link CarritoDeCompra} — sin Spring, sin base: es el mismo objeto de
 * siempre, solo que instanciado a mano en vez de por el contenedor. El {@code @SessionScope}
 * (una instancia por sesion, administrada por el contenedor) no se prueba aca porque no es logica
 * de esta clase sino un comportamiento que Spring agrega por fuera; se demuestra en vivo con dos
 * sesiones HTTP distintas, no con un test unitario.
 *
 * <p>El constructor con {@link Duration} explicita es lo que permite probar el vencimiento sin
 * esperar 5 minutos reales.
 */
class CarritoDeCompraTest {

    private static final Long COMPRADOR = 1L;
    private static final Long OTRO_COMPRADOR = 2L;

    @Test
    void agregarUnItemLoMuestraConSuPrecioCongelado() {
        CarritoDeCompra carrito = new CarritoDeCompra(Duration.ofMinutes(5));

        carrito.agregarItem(COMPRADOR, 10L, 100L, "General", new BigDecimal("15000.00"), 2);

        CarritoDTO dto = carrito.verComo(COMPRADOR);
        assertThat(dto.items()).containsExactly(
                new ItemDeCarritoDTO(10L, 100L, "General", new BigDecimal("15000.00"), 2));
        assertThat(dto.total()).isEqualByComparingTo("30000.00");
        assertThat(dto.expirado()).isFalse();
    }

    @Test
    void agregarElMismoTipoDeEntradaDosVecesSumaLaCantidadEnVezDeDuplicarLaLinea() {
        CarritoDeCompra carrito = new CarritoDeCompra(Duration.ofMinutes(5));

        carrito.agregarItem(COMPRADOR, 10L, 100L, "General", new BigDecimal("15000.00"), 2);
        carrito.agregarItem(COMPRADOR, 10L, 100L, "General", new BigDecimal("15000.00"), 3);

        List<ItemDeCarritoDTO> items = carrito.verComo(COMPRADOR).items();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).cantidad()).isEqualTo(5);
    }

    @Test
    void unaOperacionDeOtroCompradorSobreElMismoCarritoLanzaCarritoDeOtroComprador() {
        CarritoDeCompra carrito = new CarritoDeCompra(Duration.ofMinutes(5));
        carrito.agregarItem(COMPRADOR, 10L, 100L, "General", new BigDecimal("15000.00"), 1);

        assertThatThrownBy(() -> carrito.verComo(OTRO_COMPRADOR))
                .isInstanceOf(CarritoDeOtroCompradorException.class);
    }

    @Test
    void confirmarUnCarritoVacioLanzaCarritoVacioException() {
        CarritoDeCompra carrito = new CarritoDeCompra(Duration.ofMinutes(5));

        assertThatThrownBy(() -> carrito.itemsParaConfirmar(COMPRADOR))
                .isInstanceOf(CarritoVacioException.class);
    }

    @Test
    void confirmarUnCarritoVencidoLanzaCarritoVencidoException() throws InterruptedException {
        CarritoDeCompra carrito = new CarritoDeCompra(Duration.ofMillis(20));
        carrito.agregarItem(COMPRADOR, 10L, 100L, "General", new BigDecimal("15000.00"), 1);

        Thread.sleep(50);

        assertThatThrownBy(() -> carrito.itemsParaConfirmar(COMPRADOR))
                .isInstanceOf(CarritoVencidoException.class);
        // Vencido para el negocio no significa destruido: ver() sigue mostrando el carrito,
        // con el flag "expirado" en true (CarritoDTO, Javadoc).
        assertThat(carrito.verComo(COMPRADOR).expirado()).isTrue();
    }

    @Test
    void vaciarDejaElCarritoListoParaUnaCompraNueva() {
        CarritoDeCompra carrito = new CarritoDeCompra(Duration.ofMinutes(5));
        carrito.agregarItem(COMPRADOR, 10L, 100L, "General", new BigDecimal("15000.00"), 1);

        carrito.vaciar(COMPRADOR);

        assertThat(carrito.verComo(COMPRADOR).items()).isEmpty();
        assertThatThrownBy(() -> carrito.itemsParaConfirmar(COMPRADOR))
                .isInstanceOf(CarritoVacioException.class);
    }
}
