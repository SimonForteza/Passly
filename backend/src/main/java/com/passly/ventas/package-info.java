/**
 * <b>ServicioDeVentas</b>: el Facade de la compra. Mantiene el carrito temporal (~5 min) mientras
 * el comprador paga y ejecuta, en una transaccion declarativa, el descuento de cupo, el registro
 * de la orden y el cobro.
 *
 * <p><b>El componente con mas dependencias salientes del sistema (CLAUDE.md 4.6).</b> Depende de
 * {@code eventos} — para conocer precio y cupo, y para descontarlo al confirmar — y de
 * {@code usuarios} — para resolver los datos del comprador que muestra {@code OrdenDTO}, igual
 * que Eventos resuelve el organizador. <b>No depende de {@code productoras}</b>: el nombre
 * comercial del organizador ya viaja dentro de {@code EventoDTO.organizador()}, asi que declarar
 * esa arista solo sumaria una dependencia que nunca se usa.
 *
 * <p>Con este modulo el grafo de negocio deja de ser una cadena y pasa a ser un DAG:
 * {@code ventas} llega a Usuarios por dos caminos, uno directo y otro via Eventos-Productoras.
 * {@code EstructuraDeModulosTest} lo documenta.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
                "eventos", "eventos :: dto",
                "usuarios", "usuarios :: dto"})
package com.passly.ventas;
