package com.passly.productoras.internal.datos;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface InvitacionRepository extends JpaRepository<Invitacion,Long> {
    List<Invitacion> findByProductoraIdOrderByIdDesc(Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invitacion i where i.tokenHash=:hash")
    Optional<Invitacion> bloquearPorToken(@Param("hash") String hash);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invitacion i where i.id=:id")
    Optional<Invitacion> bloquear(@Param("id") Long id);
    Optional<Invitacion> findByTokenHash(String hash);
}
