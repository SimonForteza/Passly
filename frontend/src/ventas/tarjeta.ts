import type { DatosDeTarjetaDTO } from './types';

export const TARJETA_VACIA: DatosDeTarjetaDTO = {
  numero: '',
  titular: '',
  vencimiento: '',
  codigoSeguridad: '',
};

const REGEX_NUMERO = /^\d{13,19}$/;
const REGEX_VENCIMIENTO = /^(0[1-9]|1[0-2])\/\d{2}$/;
const REGEX_CODIGO_SEGURIDAD = /^\d{3,4}$/;

/** Saca los espacios de agrupación antes de mandar el numero al backend, que no los acepta. */
export function digitosDeNumero(numero: string): string {
  return numero.replace(/\s+/g, '');
}

/**
 * Espejo en el cliente de las validaciones de `DatosDeTarjetaDTO` en el backend (ventas), para
 * no pegarle a la red con un dato que ya sabemos invalido. El backend sigue siendo la autoridad:
 * si de todos modos devuelve un 400, sus mensajes por campo pisan a estos (ver CarritoPage).
 */
export function validarTarjeta(tarjeta: DatosDeTarjetaDTO): Record<string, string> {
  const errores: Record<string, string> = {};

  if (!REGEX_NUMERO.test(digitosDeNumero(tarjeta.numero))) {
    errores.numero = 'El numero de tarjeta debe tener entre 13 y 19 digitos.';
  }
  if (!tarjeta.titular.trim()) {
    errores.titular = 'El titular es obligatorio.';
  }
  if (!REGEX_VENCIMIENTO.test(tarjeta.vencimiento)) {
    errores.vencimiento = 'El vencimiento tiene formato MM/AA.';
  }
  if (!REGEX_CODIGO_SEGURIDAD.test(tarjeta.codigoSeguridad)) {
    errores.codigoSeguridad = 'El codigo de seguridad debe tener 3 o 4 digitos.';
  }

  return errores;
}

/**
 * El 400 de `POST /api/ventas/ordenes` anida la tarjeta bajo `ConfirmarCompraRequest`, asi que
 * Spring devuelve las claves del mapa `errores` con el prefijo del campo contenedor (p.ej.
 * `tarjeta.numero`, no `numero`) — confirmado contra el backend real, no asumido. Hay que
 * sacarlo para que coincida con las claves que usa `FormularioTarjeta` (`numero`, `titular`,
 * `vencimiento`, `codigoSeguridad`).
 */
export function quitarPrefijoTarjeta(errores: Record<string, string>): Record<string, string> {
  const sinPrefijo: Record<string, string> = {};
  for (const [clave, mensaje] of Object.entries(errores)) {
    sinPrefijo[clave.startsWith('tarjeta.') ? clave.slice('tarjeta.'.length) : clave] = mensaje;
  }
  return sinPrefijo;
}

/** Agrupa de a 4 mientras se tipea (formato visual tipo "4242 4242 4242 4242"). */
export function formatearNumeroTarjeta(valor: string): string {
  const digitos = valor.replace(/\D/g, '').slice(0, 19);
  return digitos.replace(/(\d{4})(?=\d)/g, '$1 ');
}

/** Inserta "/" despues de los primeros 2 digitos (formato MM/AA mientras se tipea). */
export function formatearVencimiento(valor: string): string {
  const digitos = valor.replace(/\D/g, '').slice(0, 4);
  if (digitos.length <= 2) return digitos;
  return `${digitos.slice(0, 2)}/${digitos.slice(2)}`;
}
