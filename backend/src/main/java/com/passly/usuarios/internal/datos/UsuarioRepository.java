package com.passly.usuarios.internal.datos;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Patron DAO / Repository sobre {@link Usuario}.
 *
 * <p>Expone el almacenamiento como una coleccion de objetos de dominio, de modo que la capa de
 * negocio no sabe que abajo hay una tabla. Spring Data genera la implementacion por proxy en
 * runtime — que no exista una clase concreta no cambia que el patron es DAO.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Guard de unicidad del alta: no puede haber dos cuentas con el mismo email. */
    boolean existsByEmail(String email);

    /**
     * Busqueda por la clave natural de la cuenta. El email es {@code unique} en la tabla, asi que
     * devuelve como maximo una fila. La usa la autenticacion de PAS-6 para resolver el login.
     */
    Optional<Usuario> findByEmail(String email);

    /** Listado estable para la demo y la consulta: por email ascendente. */
    List<Usuario> findAllByOrderByEmailAsc();
}
