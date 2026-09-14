package com.passly.eventos.internal.datos;

import com.passly.eventos.CupoInsuficienteException;
import com.passly.eventos.EdicionDeEventoInvalidaException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.math.BigDecimal;

/**
 * Un tipo de entrada de un evento: general, VIP, early bird.
 *
 * <p>No es raiz de agregado: se crea, se persiste y se borra a traves de su {@link Evento}.
 */
@Entity
@Table(
        name = "tipo_entrada",
        schema = "eventos",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tipo_entrada_nombre_por_evento",
                columnNames = {"evento_id", "nombre"}
        )
)
public class TipoEntrada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @Column(nullable = false, length = 80)
    private String nombre;

    /** {@code numeric(12,2)} y {@code BigDecimal}: es dinero, nunca {@code double}. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(name = "cupo_total", nullable = false)
    private Integer cupoTotal;

    @Column(name = "cupo_disponible", nullable = false)
    private Integer cupoDisponible;

    /**
     * Bloqueo optimista. Se agrego porque {@code ServicioDeVentas} decrementa
     * {@code cupoDisponible} con concurrencia real, y sumar la columna con la tabla vacia cuesta
     * cero mientras que hacerlo con datos cargados es una migracion. Es una decision declarada, no
     * un descuido. Desde PAS-19 tambien protege la carrera entre un organizador editando o
     * ampliando el cupo (esta clase) y una compra confirmandose al mismo tiempo
     * ({@code EventoService.descontarCupo}): la segunda escritura sobre la misma fila falla con
     * {@code ObjectOptimisticLockingFailureException} en vez de perderse en silencio.
     */
    @Version
    private Long version;

    /** Requerido por JPA. No usar desde el codigo de negocio. */
    protected TipoEntrada() {
    }

    public TipoEntrada(String nombre, BigDecimal precio, Integer cupoTotal) {
        this.nombre = nombre;
        this.precio = precio;
        this.cupoTotal = cupoTotal;
        // Un tipo de entrada nace con todo su cupo libre.
        this.cupoDisponible = cupoTotal;
    }

    void asignarA(Evento evento) {
        this.evento = evento;
    }

    public boolean tieneCupo() {
        return cupoDisponible != null && cupoDisponible > 0;
    }

    /**
     * Descuenta cupo. Package-private a proposito, igual que {@link #asignarA(Evento)}: cualquier
     * cambio sobre el cupo entra por la raiz del agregado ({@link Evento#descontarCupo}), nunca
     * directo sobre este hijo.
     *
     * <p>El {@link #version} de esta fila es lo que convierte una carrera entre dos confirmaciones
     * concurrentes sobre el mismo tipo de entrada en un {@code ObjectOptimisticLockingFailureException}
     * en vez de una sobreventa silenciosa.
     *
     * @throws CupoInsuficienteException si {@code cantidad} supera {@link #cupoDisponible}
     */
    void descontar(int cantidad) {
        if (cupoDisponible < cantidad) {
            throw new CupoInsuficienteException(id, nombre, cantidad, cupoDisponible);
        }
        this.cupoDisponible -= cantidad;
    }

    /** Cuanto ya se vendio de este tipo de entrada: la diferencia entre el total y lo disponible. */
    public int vendidas() {
        return cupoTotal - cupoDisponible;
    }

    /**
     * Edita nombre, precio y cupo total (PAS-19). Package-private, igual que {@link #descontar}:
     * entra por la raiz del agregado ({@link Evento#editarTipoEntrada}), nunca directo.
     *
     * <p>El nuevo {@code cupoDisponible} se recalcula contra lo ya vendido, no se reemplaza a
     * ciegas: si el cupo total sube o baja, lo vendido sigue siendo lo vendido y solo cambia lo que
     * queda libre.
     *
     * @throws EdicionDeEventoInvalidaException si {@code nuevoCupoTotal} es menor a lo ya vendido
     */
    void editar(String nombre, BigDecimal precio, int nuevoCupoTotal) {
        int vendidas = vendidas();
        if (nuevoCupoTotal < vendidas) {
            throw new EdicionDeEventoInvalidaException(
                    "El nuevo cupo total (" + nuevoCupoTotal + ") de \"" + this.nombre
                            + "\" (id " + id + ") no puede ser menor a lo ya vendido (" + vendidas + ")");
        }
        this.nombre = nombre;
        this.precio = precio;
        this.cupoTotal = nuevoCupoTotal;
        this.cupoDisponible = nuevoCupoTotal - vendidas;
    }

    /**
     * Suma {@code cantidad} tanto al cupo total como al disponible (PAS-19). Package-private,
     * igual que {@link #descontar} y {@link #editar}: entra por la raiz del agregado
     * ({@link Evento#ampliarCupo}).
     */
    void ampliarCupo(int cantidad) {
        this.cupoTotal += cantidad;
        this.cupoDisponible += cantidad;
    }

    public Long getId() {
        return id;
    }

    public Evento getEvento() {
        return evento;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public Integer getCupoTotal() {
        return cupoTotal;
    }

    public Integer getCupoDisponible() {
        return cupoDisponible;
    }
}
