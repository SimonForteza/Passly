package com.passly.eventos.internal.datos;

import com.passly.eventos.EdicionDeEventoInvalidaException;
import com.passly.eventos.EstadoEvento;
import com.passly.eventos.TipoEntradaNoEncontradoException;
import com.passly.eventos.TransicionDeEstadoInvalidaException;
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

/**
 * Un evento. <b>Raiz del agregado</b>: los {@link TipoEntrada} no tienen ciclo de vida propio y
 * cuelgan de aca por cascada. Eso es lo que explica por que {@code crearEvento} recibe los tipos
 * de entrada anidados en vez de tener un endpoint aparte — un evento sin tipos de entrada seria
 * un agregado a medias que no se puede publicar.
 *
 * <p>La entidad <b>tiene comportamiento</b>: la transicion de estado vive en {@link #publicar()},
 * no en un {@code if} del servicio. Es evitar deliberadamente el Anemic Domain Model: la regla
 * queda donde estan los datos que protege.
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
     * Transicion {@code BORRADOR -> PUBLICADO}.
     *
     * <p>Las tres reglas viven aca, junto al estado que protegen:
     * <ol>
     *   <li>solo se publica lo que esta en borrador — republicar es un conflicto, no un no-op;</li>
     *   <li>no se publica un evento cuya fecha ya paso;</li>
     *   <li>no se publica un evento sin al menos un tipo de entrada con cupo.</li>
     * </ol>
     */
    public void publicar() {
        if (estado != EstadoEvento.BORRADOR) {
            throw new TransicionDeEstadoInvalidaException(
                    "El evento " + id + " esta en estado " + estado
                            + " y solo se puede publicar un evento en BORRADOR");
        }
        if (fechaHora.isBefore(OffsetDateTime.now())) {
            throw new TransicionDeEstadoInvalidaException(
                    "No se puede publicar el evento " + id + " porque su fecha ya paso");
        }
        boolean hayCupo = tiposEntrada.stream().anyMatch(TipoEntrada::tieneCupo);
        if (!hayCupo) {
            throw new TransicionDeEstadoInvalidaException(
                    "No se puede publicar el evento " + id
                            + " porque no tiene ningun tipo de entrada con cupo disponible");
        }
        this.estado = EstadoEvento.PUBLICADO;
    }

    /**
     * Descuenta cupo de uno de los tipos de entrada de este evento.
     *
     * <p>Espejo de {@link #publicar()}: el descuento entra por la raiz del agregado, que busca el
     * hijo y delega en el la invariante ({@link TipoEntrada#descontar}). No es
     * {@code tipoEntradaRepository.findById(...).descontar(...)} porque eso saltearia la raiz —
     * exactamente lo que el Javadoc de {@code TipoEntradaRepository} pide evitar.
     *
     * @throws TipoEntradaNoEncontradoException si el tipo de entrada no es de este evento
     * @throws com.passly.eventos.CupoInsuficienteException si no queda cupo para {@code cantidad}
     */
    public void descontarCupo(Long idTipoEntrada, int cantidad) {
        buscarTipoEntrada(idTipoEntrada).descontar(cantidad);
    }

    /**
     * Edita nombre, descripcion, fecha/hora y lugar del evento (PAS-19).
     *
     * <p>Solo valida en {@link EstadoEvento#BORRADOR}: publicado, esos datos ya son la cartelera
     * que alguien vio o compro — cambiarlos por debajo seria alterar lo que ya se le mostro o
     * vendio a un comprador.
     *
     * @throws EdicionDeEventoInvalidaException si el evento no esta en BORRADOR
     */
    public void editarDatos(String nombre, String descripcion, OffsetDateTime fechaHora, String lugar) {
        exigirBorrador("editar los datos");
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.fechaHora = fechaHora;
        this.lugar = lugar;
    }

    /**
     * Agrega un tipo de entrada nuevo, con todo su cupo libre (PAS-19).
     *
     * <p>A diferencia de {@link #editarDatos}, se admite tanto en {@code BORRADOR} como en
     * {@code PUBLICADO}: sumar una opcion nueva no perjudica a nadie que ya compro. Reusa
     * {@link #agregarTipoEntrada(TipoEntrada)}, el mismo metodo que arma el agregado al crear el
     * evento.
     *
     * @throws EdicionDeEventoInvalidaException si el evento esta {@code CANCELADO}, o si ya existe
     *         un tipo de entrada con ese nombre en este evento
     */
    public void sumarTipoEntrada(String nombre, BigDecimal precio, Integer cupoTotal) {
        exigirNoCancelado("agregar un tipo de entrada");
        exigirNombreLibre(nombre, null);
        agregarTipoEntrada(new TipoEntrada(nombre, precio, cupoTotal));
    }

    /**
     * Edita nombre, precio y cupo total de un tipo de entrada existente (PAS-19).
     *
     * <p>Solo valida en {@link EstadoEvento#BORRADOR}, igual que {@link #editarDatos} — publicado,
     * para sumar cupo esta {@link #ampliarCupo}, que si admite ese estado.
     *
     * @throws TipoEntradaNoEncontradoException si el tipo de entrada no es de este evento
     * @throws EdicionDeEventoInvalidaException si el evento no esta en BORRADOR, si el nuevo cupo
     *         total es menor a lo ya vendido, o si el nombre ya lo usa otro tipo de entrada de este
     *         evento
     */
    public void editarTipoEntrada(Long idTipoEntrada, String nombre, BigDecimal precio, Integer cupoTotal) {
        exigirBorrador("editar un tipo de entrada");
        // Se busca el tipo de entrada antes de validar el nombre: si idTipoEntrada no es de este
        // evento, el error tiene que ser "no existe" (404) y no "nombre repetido" (409), aunque el
        // nombre pedido coincida con el de otro tipo de entrada real de este evento.
        TipoEntrada tipoEntrada = buscarTipoEntrada(idTipoEntrada);
        exigirNombreLibre(nombre, idTipoEntrada);
        tipoEntrada.editar(nombre, precio, cupoTotal);
    }

    /**
     * Suma cupo a un tipo de entrada existente (PAS-19). Se admite en {@code BORRADOR} o
     * {@code PUBLICADO} — es la unica edicion de tipo de entrada que un evento publicado admite.
     *
     * @throws TipoEntradaNoEncontradoException si el tipo de entrada no es de este evento
     * @throws EdicionDeEventoInvalidaException si el evento esta {@code CANCELADO}
     */
    public void ampliarCupo(Long idTipoEntrada, int cantidad) {
        exigirNoCancelado("ampliar el cupo de un tipo de entrada");
        buscarTipoEntrada(idTipoEntrada).ampliarCupo(cantidad);
    }

    private TipoEntrada buscarTipoEntrada(Long idTipoEntrada) {
        return tiposEntrada.stream()
                .filter(t -> t.getId().equals(idTipoEntrada))
                .findFirst()
                .orElseThrow(() -> new TipoEntradaNoEncontradoException(idTipoEntrada));
    }

    /** @throws EdicionDeEventoInvalidaException si el nombre ya lo usa otro tipo de entrada del evento */
    private void exigirNombreLibre(String nombre, Long idAExcluir) {
        boolean repetido = tiposEntrada.stream()
                .anyMatch(t -> !t.getId().equals(idAExcluir) && t.getNombre().equalsIgnoreCase(nombre));
        if (repetido) {
            throw new EdicionDeEventoInvalidaException(
                    "El evento " + id + " ya tiene un tipo de entrada llamado \"" + nombre + "\"");
        }
    }

    private void exigirBorrador(String accion) {
        if (estado != EstadoEvento.BORRADOR) {
            throw new EdicionDeEventoInvalidaException(
                    "No se puede " + accion + " del evento " + id + " porque esta en estado "
                            + estado + " y solo se admite en BORRADOR");
        }
    }

    private void exigirNoCancelado(String accion) {
        if (estado == EstadoEvento.CANCELADO) {
            throw new EdicionDeEventoInvalidaException(
                    "No se puede " + accion + " del evento " + id + " porque esta CANCELADO");
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
