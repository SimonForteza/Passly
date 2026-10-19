package com.passly.ventas.internal.datos;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
@Entity
@Table(name="intento_compra",schema="ventas")
public class IntentoCompra {
    @Id @Column(length=36) public String clave;
    @Column(nullable=false,length=64) public String propietario;
    @Column(nullable=false,length=64) public String huella;
    @Column(nullable=false) public String estado="PROCESANDO";
    public Long ordenId;
    public OffsetDateTime creadoEn=OffsetDateTime.now();
    protected IntentoCompra() {}
}
