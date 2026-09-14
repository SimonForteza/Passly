// Formatos compartidos por las pantallas de Ventas. Los importes llegan del backend como
// BigDecimal serializado a number; en pantalla van sin decimales, como en el Figma ("$44.000").

const FORMATO_PRECIO = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 2 });

export function formatearPrecio(importe: number): string {
  return `$${FORMATO_PRECIO.format(importe)}`;
}

const FORMATO_FECHA_EVENTO = new Intl.DateTimeFormat('es-AR', {
  weekday: 'long',
  day: '2-digit',
  month: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
});

// "sábado, 12/09, 23:00" -> "Sábado 12/09 · 23:00", el formato de las tarjetas del diseño.
export function formatearFechaEvento(fechaIso: string): string {
  const partes = FORMATO_FECHA_EVENTO.formatToParts(new Date(fechaIso));
  const valor = (tipo: Intl.DateTimeFormatPartTypes) => partes.find((parte) => parte.type === tipo)?.value ?? '';
  const dia = valor('weekday');
  return `${dia.charAt(0).toUpperCase()}${dia.slice(1)} ${valor('day')}/${valor('month')} · ${valor('hour')}:${valor('minute')}`;
}

const FORMATO_FECHA_ORDEN = new Intl.DateTimeFormat('es-AR', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
});

export function formatearFechaOrden(fechaIso: string): string {
  return FORMATO_FECHA_ORDEN.format(new Date(fechaIso));
}

const FORMATO_MES = new Intl.DateTimeFormat('es-AR', { month: 'short' });

export function diaYMes(fechaIso: string): { dia: string; mes: string } {
  const fecha = new Date(fechaIso);
  return {
    dia: String(fecha.getDate()).padStart(2, '0'),
    mes: FORMATO_MES.format(fecha).replace('.', ''),
  };
}

export function pluralEntradas(cantidad: number): string {
  return `${cantidad} ${cantidad === 1 ? 'entrada' : 'entradas'}`;
}
