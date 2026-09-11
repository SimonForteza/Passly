package com.passly.productoras.internal.datos;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Patron DAO / Repository sobre {@link Productora}.
 *
 * <p>No hay repositorio de {@link Miembro}: no es raiz de agregado, y darle uno propio permitiria
 * cargar y guardar membresias salteandose a la productora, que es justamente lo que el agregado
 * existe para evitar. Se accede a los miembros a traves de la raiz.
 */
public interface ProductoraRepository extends JpaRepository<Productora, Long> {

    /** Guard de unicidad del alta: dos productoras no pueden compartir nombre comercial. */
    boolean existsByNombreComercial(String nombreComercial);

    /** Listado estable para la cartelera publica: por nombre comercial ascendente. */
    List<Productora> findAllByOrderByNombreComercialAsc();

    /**
     * Las productoras de las que un usuario es miembro, con cualquier rol interno.
     *
     * <p>Navega la coleccion {@code miembros} hasta su campo {@code usuarioId}. El join es
     * <b>dentro</b> del esquema {@code productoras}, asi que no viola la regla de no unir esquemas
     * de componentes distintos (CLAUDE.md 4.8).
     */
    List<Productora> findByMiembros_UsuarioIdOrderByNombreComercialAsc(Long usuarioId);
}
