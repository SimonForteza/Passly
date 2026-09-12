/**
 * Objetos de transferencia del componente: la unica forma en que los datos de Pagos cruzan su
 * frontera.
 *
 * <p>Spring Modulith cierra los modulos por defecto — todo sub-paquete es interno salvo que se
 * lo declare {@code @NamedInterface}, este o no marcado como {@code internal}. Sin esta
 * anotacion, {@code com.passly.pagos.dto} seria interno y el build fallaria el dia que
 * {@code ServicioDeVentas} importe {@code ResultadoDeCobroDTO}.
 *
 * <p>Se declara desde el primer dia, aunque hoy nada la consuma todavia, siguiendo el mismo
 * criterio que {@code eventos.dto} y {@code usuarios.dto}: no descubrirlo mas adelante.
 */
@org.springframework.modulith.NamedInterface("dto")
package com.passly.pagos.dto;
