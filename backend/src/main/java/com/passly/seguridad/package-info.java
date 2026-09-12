/**
 * Componente transversal de <b>seguridad</b>: autenticacion HTTP Basic y el cableado de la
 * autorizacion por rol (PAS-6).
 *
 * <p>No expone contrato publico: es infraestructura. Toda su implementacion vive en
 * {@code internal} — el {@code SecurityFilterChain}, el {@code UserDetailsService} y el
 * {@code AuthenticationProvider}. Las reglas <i>por rol</i> ({@code @PreAuthorize}) no viven aca:
 * se declaran sobre los controllers de cada componente, que es lo que guarda cada operacion
 * sensible en su punto de entrada.
 *
 * <p><b>Dependencias:</b> {@code seguridad} depende de {@code usuarios} — de su API publica
 * ({@code UsuarioService}) y del contrato {@code usuarios :: autenticacion}
 * ({@code CredencialDTO}) — para resolver quien es quien. Reutiliza ademas el
 * {@code PasswordEncoder} que declara {@code usuarios} (inyectado por tipo), de modo que hashear
 * y verificar usen siempre el mismo algoritmo, sin un segundo bean.
 */
package com.passly.seguridad;
