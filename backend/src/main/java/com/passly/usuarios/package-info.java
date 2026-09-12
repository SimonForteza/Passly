/**
 * <b>ServicioDeUsuarios</b>: identidad, credenciales y rol global de las personas del sistema.
 *
 * <p><b>Usuarios es la raiz del grafo de dependencias</b> y esta anotacion lo convierte en algo que
 * el build verifica. La lista vacia de dependencias permitidas no significa "sin restricciones":
 * significa <i>ninguna</i>. Si manana alguien importa aca cualquier otro modulo del sistema, el
 * build falla con {@code Allowed targets: none}.
 *
 * <p>Que Usuarios no dependa de nadie es lo que le permite ser el cimiento de la cadena
 * {@code eventos -> productoras -> usuarios}: la identidad es lo unico que el resto del sistema
 * necesita sin tener nada que ofrecer a cambio. Si Usuarios llegara a necesitar a otro componente,
 * seria una senal de que ese componente esta mal ubicado — y el build lo diria antes que una
 * revision de codigo.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {})
package com.passly.usuarios;
