package com.passly.pagos.internal.datos;

import com.passly.pagos.EstadoDePago;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Registro de un intento de cobro contra la pasarela externa, se haya aprobado o rechazado.
 *
 * <p>Se persiste en los dos casos, no solo cuando se aprueba: es lo que despues permite conciliar
 * contra el resumen de la pasarela, y es evidencia de que el Adapter efectivamente hablo con un
 * proveedor externo real (por HTTP), no que devolvio una respuesta enlatada.
 *
 * <p><b>Deliberadamente no guarda numero de tarjeta ni codigo de seguridad.</b> Esos datos son
 * transitorios: existen solo para armar el pedido a la pasarela
 * ({@code internal.SolicitudExternaDeCobro}) y se descartan apenas se usan. Es el mismo criterio
 * que {@code usuarios} aplica a la contraseña — un dato sensible no persiste mas alla de donde
 * hace falta.
 *
 * <p><b>Sobre la visibilidad:</b> es {@code public} por la misma razon que {@code eventos.Evento}
 * (CLAUDE.md 4.3): la usa {@code PagoServiceImpl}, que vive en {@code internal.negocio}, y la
 * visibilidad de paquete de Java no es jerarquica. Quien impone la frontera real es Spring
 * Modulith, no el compilador.
 */
@Entity
@Table(name = "pago", schema = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** {@code numeric(12,2)} y {@code BigDecimal}: es dinero, nunca {@code double}. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 3)
    private String moneda;

    /** La referencia de quien pidio el cobro (hoy quien llama al endpoint; mañana, una orden). */
    @Column(nullable = false, length = 100)
    private String referencia;

    @Column(name = "id_transaccion_externa", nullable = false, length = 100)
    private String idTransaccionExterna;

    /** STRING y no ORDINAL: agregar un estado nuevo no debe corromper las filas existentes. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoDePago estado;

    @Column(name = "fecha_procesamiento", nullable = false)
    private OffsetDateTime fechaProcesamiento;

    /** Requerido por JPA. No usar desde el codigo de negocio. */
    protected Pago() {
    }

    public Pago(
            BigDecimal monto,
            String moneda,
            String referencia,
            String idTransaccionExterna,
            EstadoDePago estado,
            OffsetDateTime fechaProcesamiento
    ) {
        this.monto = monto;
        this.moneda = moneda;
        this.referencia = referencia;
        this.idTransaccionExterna = idTransaccionExterna;
        this.estado = estado;
        this.fechaProcesamiento = fechaProcesamiento;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public String getMoneda() {
        return moneda;
    }

    public String getReferencia() {
        return referencia;
    }

    public String getIdTransaccionExterna() {
        return idTransaccionExterna;
    }

    public EstadoDePago getEstado() {
        return estado;
    }

    public OffsetDateTime getFechaProcesamiento() {
        return fechaProcesamiento;
    }
}
