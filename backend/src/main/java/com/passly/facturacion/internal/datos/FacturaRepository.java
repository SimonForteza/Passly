package com.passly.facturacion.internal.datos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public interface FacturaRepository extends JpaRepository<Factura, Long> {
    // No usar exists + save: dos consumidores pueden competir por la misma orden.
    // ON CONFLICT evita ademas dejar la transaccion abortada por una violacion de unicidad.
    @Modifying
    @Query(value = """
        INSERT INTO facturacion.factura (orden_id, comprador_id, total, cobrada_en, estado, creada_en)
        VALUES (:orden, :comprador, :total, :cobrada, 'PENDIENTE_AFIP', CURRENT_TIMESTAMP)
        ON CONFLICT (orden_id) DO NOTHING
        """, nativeQuery = true)
    int insertarSiNoExiste(@Param("orden") Long orden, @Param("comprador") Long comprador,
                          @Param("total") BigDecimal total, @Param("cobrada") OffsetDateTime cobrada);

    @Modifying
    @Query(value="""
        INSERT INTO facturacion.factura (orden_id,comprador_id,comprador_nombre,comprador_email,total,cobrada_en,estado,creada_en)
        VALUES (:orden,:comprador,:nombre,:email,:total,:cobrada,'PENDIENTE_AFIP',CURRENT_TIMESTAMP)
        ON CONFLICT (orden_id) DO NOTHING
        """,nativeQuery=true)
    int insertarInvitadoSiNoExiste(@Param("orden") Long orden,@Param("comprador") Long comprador,
            @Param("nombre") String nombre,@Param("email") String email,@Param("total") BigDecimal total,
            @Param("cobrada") OffsetDateTime cobrada);
}
