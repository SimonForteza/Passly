package com.passly.notificaciones.internal;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
interface CorreoRepository extends JpaRepository<Correo,Long> {
    @Query(value="select * from notificaciones.correo where estado <> 'ENVIADO' and proximo_intento <= :ahora order by id limit 10 for update skip locked", nativeQuery=true)
    List<Correo> pendientes(@org.springframework.data.repository.query.Param("ahora") OffsetDateTime ahora);
}
