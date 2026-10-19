package com.passly.ventas.internal.negocio;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import java.util.*;
@Component @SessionScope
class CarritoInvitado {
    private final Object candado=new Object();
    Object candado() { return candado; }
    String propietario() { return propietario; }
    CarritoDeCompra carrito() { return carrito; }
    Map<String,String> huellas() { return huellas; }
    final String propietario=SeguridadEnlaces.token();
    CarritoDeCompra carrito=new CarritoDeCompra();
    final Map<String,String> tokens=new HashMap<>();
    final Map<String,String> huellas=new HashMap<>();
    String tokenPara(String clave) { return tokens.computeIfAbsent(clave,k->SeguridadEnlaces.token()); }
    void reiniciar() { carrito=new CarritoDeCompra(); }
}
