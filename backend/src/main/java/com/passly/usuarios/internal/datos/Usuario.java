package com.passly.usuarios.internal.datos;

import com.passly.usuarios.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un usuario del sistema y su credencial.
 *
 * <p><b>Sobre la credencial:</b> se guarda unicamente el hash BCrypt en {@code passwordHash};
 * el password en texto plano no se persiste ni se loguea nunca. Ese hash <b>no cruza la frontera
 * del componente</b>: {@code UsuarioMapper} arma un {@code UsuarioDTO} que no lo incluye. Es la
 * contracara, en la capa de datos, de la regla "las entidades JPA nunca salen del componente"
 * (CLAUDE.md 4.3).
 *
 * <p><b>Sobre la visibilidad:</b> esta clase es {@code public} porque la usa
 * {@code UsuarioServiceImpl}, que vive en {@code ...internal.negocio} — y la visibilidad de
 * paquete de Java <b>no es jerarquica</b>: {@code internal.negocio} e {@code internal.datos} son
 * paquetes distintos y no se ven entre si. Que sea {@code public} no la vuelve parte del
 * contrato: quien impone la frontera es Spring Modulith, que hace fallar el build si otro modulo
 * la importa.
 */
@Entity
@Table(name = "usuario", schema = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identidad y, desde PAS-6, credencial de login. {@code unique}: no puede haber dos cuentas
     * con el mismo mail. 254 es el largo maximo de un email segun la RFC 5321.
     */
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(nullable = false, length = 150)
    private String nombre;

    /**
     * Hash BCrypt de la contrasena, nunca el texto plano. BCrypt genera 60 caracteres; 72 deja
     * margen. El getter existe para que el propio componente pueda verificar el login (PAS-6),
     * pero {@code UsuarioMapper} no lo lee: el hash no aparece en ningun DTO ni sale del modulo.
     */
    @Column(name = "password_hash", nullable = false, length = 72)
    private String passwordHash;

    /** STRING y no ORDINAL: agregar un rol nuevo no debe corromper las filas existentes. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    /** Requerido por JPA. No usar desde el codigo de negocio. */
    protected Usuario() {
    }

    public Usuario(String email, String nombre, String passwordHash, Rol rol) {
        this.email = email;
        this.nombre = nombre;
        this.passwordHash = passwordHash;
        this.rol = rol;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getNombre() {
        return nombre;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Rol getRol() {
        return rol;
    }
}
