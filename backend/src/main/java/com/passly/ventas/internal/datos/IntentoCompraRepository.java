package com.passly.ventas.internal.datos;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface IntentoCompraRepository extends JpaRepository<IntentoCompra,String> {
    @Modifying
    @Query(value="insert into ventas.intento_compra (clave,propietario,huella,estado,creado_en) values (:clave,:propietario,:huella,'PROCESANDO',current_timestamp) on conflict (clave) do nothing",nativeQuery=true)
    int iniciar(@Param("clave") String clave,@Param("propietario") String propietario,@Param("huella") String huella);
}
