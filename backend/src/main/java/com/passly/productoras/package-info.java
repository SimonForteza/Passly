/**
 * <b>ServicioDeProductoras</b>: las identidades comerciales que publican fiestas y el padron de
 * quienes pueden operar en nombre de cada una.
 *
 * <p><b>Que hace esta anotacion:</b> declara que Productoras solo puede depender de Usuarios, y de
 * el solo su paquete base y su interfaz nombrada {@code dto}. Si manana alguien importa desde aca
 * cualquier otro modulo — o los {@code internal} de Usuarios — <b>el build falla</b>. La frontera
 * deja de ser una convencion del equipo y pasa a ser una condicion de compilacion.
 *
 * <p>Con esto el grafo del sistema queda declarado como la cadena
 * {@code eventos -> productoras -> usuarios}, con Usuarios como raiz.
 * {@code EstructuraDeModulosTest} lo verifica en cada build.
 *
 * <p><b>Para que sirve en la practica:</b> Productoras necesita a Usuarios para una sola cosa —
 * validar que quien entra al padron existe y que su rol global es compatible con el rol interno
 * pedido. Esa es toda la superficie de contacto, y esta anotacion la fija por escrito.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"usuarios", "usuarios :: dto"})
package com.passly.productoras;
