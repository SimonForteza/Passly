package com.passly.productoras.internal.datos;

import com.passly.productoras.RolEnProductora;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;

/**
 * La pertenencia de un usuario a una productora, con el rol que tiene adentro.
 *
 * <p><b>No es raiz de agregado</b>: no tiene ciclo de vida propio y cuelga de {@link Productora}
 * por cascada, igual que {@code TipoEntrada} cuelga de {@code Evento}. Una membresia sin productora
 * no significa nada.
 *
 * <p><b>Sobre {@code usuarioId} sin clave foranea:</b> apunta a {@code usuarios.usuario.id}, que
 * vive en el esquema de otro componente. No hay FK fisica a proposito — CLAUDE.md 4.8 prohibe los
 * joins entre esquemas de componentes distintos. La integridad la impone el negocio, que antes de
 * crear la membresia le pregunta a {@code UsuarioService} si el usuario existe y si su rol global
 * es compatible con el rol interno pedido. Es una <b>referencia por id entre bounded contexts</b>:
 * la restriccion la aplica el codigo que conoce la regla, no el motor de la base.
 *
 * <p><b>Sobre la visibilidad:</b> es {@code public} porque la usa {@code ProductoraServiceImpl},
 * que vive en otro paquete ({@code internal.negocio}) y la visibilidad de paquete de Java no es
 * jerarquica. Quien impone la frontera real es Spring Modulith, no el compilador.
 */
@Entity
@Table(
        name = "miembro",
        schema = "productoras",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_miembro_usuario_por_productora",
                columnNames = {"productora_id", "usuario_id"})
)
public class Miembro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "productora_id", nullable = false)
    private Productora productora;

    /** Id de {@code usuarios.usuario}. Sin FK: ver el javadoc de la clase. */
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    /** STRING y no ORDINAL: agregar un rol nuevo no debe corromper las filas existentes. */
    @Enumerated(EnumType.STRING)
    @Column(name = "rol_interno", nullable = false, length = 20)
    private RolEnProductora rolInterno;

    @Column(name = "agregado_en", nullable = false)
    private OffsetDateTime agregadoEn;

    /** Requerido por JPA. No usar desde el codigo de negocio. */
    protected Miembro() {
    }

    public Miembro(Long usuarioId, RolEnProductora rolInterno) {
        this.usuarioId = usuarioId;
        this.rolInterno = rolInterno;
        this.agregadoEn = OffsetDateTime.now();
    }

    /** Mantiene sincronizado el extremo dueno de la relacion. Lo llama {@link Productora}. */
    void asignarA(Productora productora) {
        this.productora = productora;
    }

    public boolean esDelUsuario(Long idUsuario) {
        return this.usuarioId.equals(idUsuario);
    }

    public boolean puedeGestionarEventos() {
        return rolInterno.puedeGestionarEventos();
    }

    public boolean puedeGestionarMiembros() {
        return rolInterno.puedeGestionarMiembros();
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public RolEnProductora getRolInterno() {
        return rolInterno;
    }

    public OffsetDateTime getAgregadoEn() {
        return agregadoEn;
    }
}
