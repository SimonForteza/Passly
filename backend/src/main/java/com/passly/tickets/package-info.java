/**
 * <b>ServicioDeTickets</b>: emite un ticket individual por cada entrada comprada y genera un QR
 * unico con contenido firmado. No consulta tablas ni componentes ajenos: Ventas le entrega un
 * snapshot de los datos necesarios mediante su contrato publico.
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {})
package com.passly.tickets;
