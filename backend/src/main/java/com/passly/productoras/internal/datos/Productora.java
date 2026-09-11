package com.passly.productoras.internal.datos;

import com.passly.productoras.RolEnProductora;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Una productora: la identidad <b>comercial</b> que publica fiestas.
 *
 * <p><b>Raiz del agregado.</b> Los {@link Miembro} cuelgan de aca por cascada y no se manipulan
 * desde afuera: quien quiera incorporar a alguien pasa por {@link #incorporar(Miembro)}, que es el
 * unico camino. Mismo patron que {@code Evento} con sus {@code TipoEntrada}.
 *
 * <p><b>Por que es un componente propio y no una tabla de Usuarios</b> (pregunta probable en el
 * oral): Usuarios custodia identidad y credenciales de personas; Productora es identidad comercial,
 * con ciclo de vida propio, nombre de fantasia, CUIT y un padron N:M de miembros. Ademas sus
 * consumidores futuros — Ventas, reportes, facturacion — no tienen nada que ver con el login.
 * Meterla en Usuarios lo volveria un god-module y le haria perder la raiz del grafo.
 *
 * <p><b>Sobre la visibilidad:</b> {@code public} porque la usa {@code ProductoraServiceImpl} desde
 * {@code internal.negocio}, que es otro paquete. Que sea publica no la vuelve parte del contrato:
 * quien impone la frontera es Spring Modulith, que falla el build si otro modulo la importa.
 */
@Entity
@Table(name = "productora", schema = "productoras")
public class Productora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_comercial", nullable = false, unique = true, length = 150)
    private String nombreComercial;

    /**
     * Solo digitos, sin guiones. Nullable: una productora puede empezar a operar en la plataforma
     * antes de tener los datos fiscales cargados.
     */
    @Column(length = 11, unique = true)
    private String cuit;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "creada_en", nullable = false)
    private OffsetDateTime creadaEn;

    @OneToMany(mappedBy = "productora", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Miembro> miembros = new ArrayList<>();

    /** Requerido por JPA. No usar desde el codigo de negocio. */
    protected Productora() {
    }

    public Productora(String nombreComercial, String cuit, String descripcion, String logoUrl) {
        this.nombreComercial = nombreComercial;
        this.cuit = cuit;
        this.descripcion = descripcion;
        this.logoUrl = logoUrl;
        this.creadaEn = OffsetDateTime.now();
    }

    /**
     * Incorpora un miembro al padron y mantiene los dos extremos de la relacion sincronizados.
     *
     * <p>No valida el rol <i>global</i> del usuario: eso exige preguntarle a {@code UsuarioService},
     * conocimiento que el agregado no tiene ni deberia tener. Ese guard vive en el servicio.
     */
    public void incorporar(Miembro miembro) {
        miembro.asignarA(this);
        this.miembros.add(miembro);
    }

    public Optional<Miembro> buscarMiembro(Long idUsuario) {
        return miembros.stream()
                .filter(miembro -> miembro.esDelUsuario(idUsuario))
                .findFirst();
    }

    public boolean tieneMiembro(Long idUsuario) {
        return buscarMiembro(idUsuario).isPresent();
    }

    /**
     * Si el usuario puede crear, editar y publicar los eventos de esta productora.
     *
     * <p>La regla vive aca y no en el servicio porque usa <b>solo datos propios del agregado</b>:
     * su padron y el rol de cada miembro. Es el mismo criterio por el que {@code Evento.publicar()}
     * esta en la entidad. Un no-miembro da {@code false}, igual que un {@code VALIDADOR}.
     */
    public boolean puedeGestionarEventos(Long idUsuario) {
        return buscarMiembro(idUsuario)
                .map(Miembro::puedeGestionarEventos)
                .orElse(false);
    }

    /** Si el usuario puede incorporar o quitar miembros del padron. Solo el DUENIO. */
    public boolean puedeGestionarMiembros(Long idUsuario) {
        return buscarMiembro(idUsuario)
                .map(Miembro::puedeGestionarMiembros)
                .orElse(false);
    }

    public boolean tieneMiembroConRol(Long idUsuario, RolEnProductora rol) {
        return buscarMiembro(idUsuario)
                .map(miembro -> miembro.getRolInterno() == rol)
                .orElse(false);
    }

    public Long getId() {
        return id;
    }

    public String getNombreComercial() {
        return nombreComercial;
    }

    public String getCuit() {
        return cuit;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public OffsetDateTime getCreadaEn() {
        return creadaEn;
    }

    public List<Miembro> getMiembros() {
        return List.copyOf(miembros);
    }
}
