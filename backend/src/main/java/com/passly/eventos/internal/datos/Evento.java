package com.passly.eventos.internal.datos;

import com.passly.eventos.CupoInsuficienteException;
import com.passly.eventos.EdicionDeEventoInvalidaException;
import com.passly.eventos.EstadoEvento;
import com.passly.eventos.TipoEntradaNoEncontradoException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Un evento. <b>Raiz del agregado</b>: los {@link TipoEntrada} no tienen ciclo de vida propio y
 * cuelgan de aca por cascada. Eso es lo que explica por que {@code crearEvento} recibe los tipos
 * de entrada anidados en vez de tener un endpoint aparte — un evento sin tipos de entrada seria
 * un agregado a medias que no se puede publicar.
 *
 * <p>La entidad <b>tiene comportamiento</b>, y lo que depende del estado no esta en {@code if}
 * sino en el <b>patron State</b> (PAS-35): cada operacion sensible al ciclo de vida delega en
 * {@link EstadoDeEvento} ({@link Borrador}, {@link Publicado}, {@link Cancelado}). La columna
 * {@code estado} sigue siendo el enum STRING; el objeto de estado se resuelve a partir de el y no
 * se persiste. Es evitar deliberadamente el Anemic Domain Model: la regla queda donde estan los
 * datos que protege.
 *
 * <p><b>Sobre la visibilidad:</b> esta clase es {@code public} porque la usa
 * {@code EventoServiceImpl}, que vive en {@code ...internal.negocio} — y la visibilidad de
 * paquete de Java <b>no es jerarquica</b>: {@code internal.negocio} e {@code internal.datos} son
 * paquetes distintos y no se ven entre si. Que sea {@code public} no la vuelve parte del
 * contrato: quien impone la frontera es Spring Modulith, que hace fallar el build si otro modulo
 * la importa.
 */
@Entity
@Table(name = "evento", schema = "eventos")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * La productora dueña del evento. Es un id de {@code productoras.productora} <b>sin clave
     * foranea</b>: cruza el esquema de otro componente, y CLAUDE.md 4.8 prohibe unir esquemas de
     * componentes distintos. Quien garantiza que apunte a una productora real es el servicio, que
     * antes de crear el evento le pregunta a {@code ProductoraService} si quien opera puede
     * gestionarla — y esa pregunta solo da {@code true} para una productora existente.
     *
     * <p>Es {@code updatable = false}: un evento no cambia de dueño. Transferirlo seria una
     * operacion de negocio con sus propias reglas, no un update de campo.
     */
    @Column(name = "productora_id", nullable = false, updatable = false)
    private Long productoraId;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(columnDefinition = "text")
    private String descripcion;

    /** {@code timestamptz}: la hora de un evento tiene zona y perderla no es recuperable. */
    @Column(name = "fecha_hora", nullable = false)
    private OffsetDateTime fechaHora;

    @Column(nullable = false, length = 200)
    private String lugar;

    /** STRING y no ORDINAL: agregar un estado nuevo no debe corromper las filas existentes. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEvento estado;

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TipoEntrada> tiposEntrada = new ArrayList<>();

    /** Requerido por JPA. No usar desde el codigo de negocio. */
    protected Evento() {
    }

    public Evento(
            Long productoraId,
            String nombre,
            String descripcion,
            OffsetDateTime fechaHora,
            String lugar
    ) {
        this.productoraId = productoraId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.fechaHora = fechaHora;
        this.lugar = lugar;
        this.estado = EstadoEvento.BORRADOR;
    }

    /**
     * Agrega un tipo de entrada al agregado y mantiene los dos extremos de la relacion
     * sincronizados.
     */
    public void agregarTipoEntrada(TipoEntrada tipoEntrada) {
        tipoEntrada.asignarA(this);
        this.tiposEntrada.add(tipoEntrada);
    }

    /**
     * Transicion {@code BORRADOR -> PUBLICADO}. La decide el estado actual ({@link Borrador}
     * valida fecha y cupo; {@link Publicado} y {@link Cancelado} la rechazan con 409).
     */
    public void publicar() {
        comportamiento().publicar(this);
    }

    /**
     * Transicion a {@code CANCELADO} (PAS-35). Valida desde {@code BORRADOR} o {@code PUBLICADO};
     * cancelar un evento ya cancelado es un conflicto.
     */
    public void cancelar() {
        comportamiento().cancelar(this);
    }

    /** Si se pueden vender entradas de este evento: solo publicado. Lo decide el estado. */
    public boolean admiteVentas() {
        return comportamiento().admiteVentas();
    }

    /**
     * Descuenta cupo de uno de los tipos de entrada de este evento.
     *
     * <p>El descuento entra por la raiz del agregado, que busca el hijo y delega en el la
     * invariante ({@link TipoEntrada#descontar}). No es
     * {@code tipoEntradaRepository.findById(...).descontar(...)} porque eso saltearia la raiz —
     * exactamente lo que el Javadoc de {@code TipoEntradaRepository} pide evitar.
     *
     * <p>Desde PAS-35 tambien es la ultima linea de defensa de {@link #admiteVentas()}: un borrador
     * o un evento cancelado no vende, aunque alguien tenga el tipo de entrada en un carrito armado
     * antes de la cancelacion. Se responde con {@link CupoInsuficienteException} porque para quien
     * compra es lo mismo — no queda nada que vender — y Ventas ya la traduce a 409.
     *
     * @throws TipoEntradaNoEncontradoException si el tipo de entrada no es de este evento
     * @throws CupoInsuficienteException si el evento no admite ventas o no queda cupo para {@code cantidad}
     */
    public void descontarCupo(Long idTipoEntrada, int cantidad) {
        TipoEntrada tipoEntrada = buscarTipoEntrada(idTipoEntrada);
        if (!admiteVentas()) {
            throw CupoInsuficienteException.porEventoSinVentas(
                    idTipoEntrada, tipoEntrada.getNombre(), cantidad, estado);
        }
        tipoEntrada.descontar(cantidad);
    }

    /**
     * Edita nombre, descripcion, fecha/hora y lugar del evento (PAS-19). Solo {@link Borrador} lo
     * admite: publicado, esos datos ya son la cartelera que alguien vio o compro.
     *
     * @throws EdicionDeEventoInvalidaException si el evento no esta en BORRADOR
     */
    public void editarDatos(String nombre, String descripcion, OffsetDateTime fechaHora, String lugar) {
        comportamiento().editarDatos(this, nombre, descripcion, fechaHora, lugar);
    }

    /**
     * Agrega un tipo de entrada nuevo, con todo su cupo libre (PAS-19). Lo admiten
     * {@link Borrador} y {@link Publicado}: sumar una opcion nueva no perjudica a nadie que ya
     * compro.
     *
     * @throws EdicionDeEventoInvalidaException si el evento esta {@code CANCELADO}, o si ya existe
     *         un tipo de entrada con ese nombre en este evento
     */
    public void sumarTipoEntrada(String nombre, BigDecimal precio, Integer cupoTotal) {
        comportamiento().agregarTipoEntrada(this, nombre, precio, cupoTotal);
    }

    /**
     * Edita nombre, precio y cupo total de un tipo de entrada existente (PAS-19). Solo
     * {@link Borrador} lo admite — publicado, para sumar cupo esta {@link #ampliarCupo}.
     *
     * @throws TipoEntradaNoEncontradoException si el tipo de entrada no es de este evento
     * @throws EdicionDeEventoInvalidaException si el evento no esta en BORRADOR, si el nuevo cupo
     *         total es menor a lo ya vendido, o si el nombre ya lo usa otro tipo de entrada de este
     *         evento
     */
    public void editarTipoEntrada(Long idTipoEntrada, String nombre, BigDecimal precio, Integer cupoTotal) {
        comportamiento().editarTipoEntrada(this, idTipoEntrada, nombre, precio, cupoTotal);
    }

    /**
     * Suma cupo a un tipo de entrada existente (PAS-19). Lo admiten {@link Borrador} y
     * {@link Publicado} — es la unica edicion de tipo de entrada que un evento publicado admite.
     *
     * @throws TipoEntradaNoEncontradoException si el tipo de entrada no es de este evento
     * @throws EdicionDeEventoInvalidaException si el evento esta {@code CANCELADO}
     */
    public void ampliarCupo(Long idTipoEntrada, int cantidad) {
        comportamiento().ampliarCupo(this, idTipoEntrada, cantidad);
    }

    // --- Lo que usan los estados -------------------------------------------------------------
    // Package-private: solo las clases de EstadoDeEvento (mismo paquete) aplican cambios. Estos
    // metodos NO chequean el estado; esa decision ya la tomo la clase de estado que los llama.

    /** El comportamiento del estado actual, resuelto a partir del enum persistido. */
    private EstadoDeEvento comportamiento() {
        return EstadoDeEvento.de(estado);
    }

    void pasarA(EstadoDeEvento nuevo) {
        this.estado = nuevo.valor();
    }

    boolean tieneAlgunTipoDeEntradaConCupo() {
        return tiposEntrada.stream().anyMatch(TipoEntrada::tieneCupo);
    }

    void aplicarDatos(String nombre, String descripcion, OffsetDateTime fechaHora, String lugar) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.fechaHora = fechaHora;
        this.lugar = lugar;
    }

    void aplicarNuevoTipoEntrada(String nombre, BigDecimal precio, Integer cupoTotal) {
        exigirNombreLibre(nombre, null);
        agregarTipoEntrada(new TipoEntrada(nombre, precio, cupoTotal));
    }

    void aplicarEdicionDeTipoEntrada(Long idTipoEntrada, String nombre, BigDecimal precio, Integer cupoTotal) {
        // Se busca el tipo de entrada antes de validar el nombre: si idTipoEntrada no es de este
        // evento, el error tiene que ser "no existe" (404) y no "nombre repetido" (409), aunque el
        // nombre pedido coincida con el de otro tipo de entrada real de este evento.
        TipoEntrada tipoEntrada = buscarTipoEntrada(idTipoEntrada);
        exigirNombreLibre(nombre, idTipoEntrada);
        tipoEntrada.editar(nombre, precio, cupoTotal);
    }

    void aplicarAmpliacionDeCupo(Long idTipoEntrada, int cantidad) {
        buscarTipoEntrada(idTipoEntrada).ampliarCupo(cantidad);
    }

    private TipoEntrada buscarTipoEntrada(Long idTipoEntrada) {
        return tiposEntrada.stream()
                .filter(t -> Objects.equals(t.getId(), idTipoEntrada))
                .findFirst()
                .orElseThrow(() -> new TipoEntradaNoEncontradoException(idTipoEntrada));
    }

    /** @throws EdicionDeEventoInvalidaException si el nombre ya lo usa otro tipo de entrada del evento */
    private void exigirNombreLibre(String nombre, Long idAExcluir) {
        boolean repetido = tiposEntrada.stream()
                .filter(t -> idAExcluir == null || !idAExcluir.equals(t.getId()))
                .anyMatch(t -> t.getNombre().equalsIgnoreCase(nombre));
        if (repetido) {
            throw new EdicionDeEventoInvalidaException(
                    "El evento " + id + " ya tiene un tipo de entrada llamado \"" + nombre + "\"");
        }
    }

    /**
     * Si el evento pertenece a esa productora.
     *
     * <p>Responde sobre la <b>pertenencia</b>, que es un dato propio del agregado. Deliberadamente
     * no responde sobre la <i>autorizacion</i>: para saber si alguien puede operar el evento hay que
     * preguntarle a Productoras quien gestiona esa productora, y eso es conocimiento externo. Por eso
     * el guard vive en el servicio y esta entidad solo aporta la mitad que le corresponde.
     */
    public boolean perteneceA(Long idProductora) {
        return this.productoraId.equals(idProductora);
    }

    public Long getId() {
        return id;
    }

    public Long getProductoraId() {
        return productoraId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public OffsetDateTime getFechaHora() {
        return fechaHora;
    }

    public String getLugar() {
        return lugar;
    }

    public EstadoEvento getEstado() {
        return estado;
    }

    public List<TipoEntrada> getTiposEntrada() {
        return List.copyOf(tiposEntrada);
    }
}
