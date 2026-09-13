/**
 * <b>ServicioDePagos</b>: Adapter REST hacia la pasarela de pago externa (PAS-7).
 *
 * <p>Aporta dos cosas al checklist: el patron <b>Adapter</b> (interfaz propia de "proveedor
 * externo" sobre un protocolo incompatible) y la integracion <b>REST saliente</b> del sistema —
 * la pasarela de pago, no la API autogenerada de Supabase (CLAUDE.md 3).
 *
 * <p><b>Pagos no depende de ningun otro componente de Passly.</b> Su unica colaboracion externa
 * es una llamada HTTP a un proveedor de pago, y eso no es una dependencia de modulo en el sentido
 * de Spring Modulith: es una llamada de red a traves de {@code RestClient}, no un import de
 * Java. La lista vacia de {@code allowedDependencies} lo deja escrito: si algun dia este modulo
 * necesitara importar {@code eventos} o {@code usuarios}, el build fallaria hasta declararlo, tal
 * como paso con Eventos al aparecer Productoras.
 *
 * <p><b>Sobre el mock:</b> el endpoint de la pasarela vive en
 * {@code internal.pasarelamock} y corre en este mismo proceso, para no pelear con una pasarela
 * real ni con certificados de homologacion — el mismo criterio que CLAUDE.md aplica a AFIP.
 * {@code PagoServiceImpl} le habla por HTTP real (no una llamada Java directa), asi que pasar a un
 * proveedor real el dia de mañana es cambiar la propiedad {@code passly.pagos.pasarela.base-url},
 * nada de codigo — el mismo patron de externalizacion que la URL de la base de datos (CLAUDE.md
 * 4.8).
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {})
package com.passly.pagos;
