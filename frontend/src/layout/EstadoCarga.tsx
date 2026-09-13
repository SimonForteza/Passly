export function EstadoCarga({ mensaje = 'Cargando...' }: { mensaje?: string }) {
  return <p style={{ color: 'var(--color-texto-secundario)' }}>{mensaje}</p>;
}
