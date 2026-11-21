/** Chips de la cartelera (Figma Web 25): filtran por la fecha que ya tiene cada evento. */
export const PERIODOS = [
  { id: 'todos', nombre: 'Todos' },
  { id: 'semana', nombre: 'Esta semana' },
  { id: 'mes', nombre: 'Este mes' },
  { id: 'proximo-mes', nombre: 'Próximo mes' },
] as const;

export type Periodo = (typeof PERIODOS)[number]['id'];

/**
 * Si la fecha cae en el período, mirado desde `ahora`.
 * - Esta semana: hasta el domingo a la noche de la semana en curso.
 * - Este mes / Próximo mes: por mes calendario.
 */
export function enPeriodo(fechaHora: string, periodo: Periodo, ahora = new Date()): boolean {
  if (periodo === 'todos') return true;
  const fecha = new Date(fechaHora);
  if (periodo === 'semana') {
    const finDeSemana = new Date(ahora);
    const diasHastaDomingo = (7 - ahora.getDay()) % 7;
    finDeSemana.setDate(ahora.getDate() + diasHastaDomingo);
    finDeSemana.setHours(23, 59, 59, 999);
    return fecha <= finDeSemana;
  }
  const desplazamiento = periodo === 'mes' ? 0 : 1;
  const objetivo = new Date(ahora.getFullYear(), ahora.getMonth() + desplazamiento, 1);
  return (
    fecha.getFullYear() === objetivo.getFullYear() && fecha.getMonth() === objetivo.getMonth()
  );
}
