package com.passly.productoras.internal.datos;
import com.passly.productoras.RolEnProductora;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
@Entity
@Table(name="invitacion",schema="productoras")
public class Invitacion {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false) public Long productoraId;
    @Column(nullable=false,length=254) public String email;
    @Enumerated(EnumType.STRING) @Column(nullable=false) public RolEnProductora rol;
    @Column(nullable=false,unique=true,length=64) public String tokenHash;
    @Column(nullable=false) public OffsetDateTime venceEn;
    @Column(nullable=false) public String estado="PENDIENTE";
    public Long correoId;
    protected Invitacion() {}
    public Invitacion(Long productoraId,String email,RolEnProductora rol) {
        this.productoraId=productoraId; this.email=email; this.rol=rol;
    }
}
