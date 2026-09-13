import { useParams } from 'react-router-dom';

export function DetalleEventoPage() {
  const { id } = useParams();

  return (
    <section>
      <h2>Detalle del evento #{id}</h2>
      <p style={{ color: 'var(--color-texto-secundario)' }}>
        Próximamente: ficha del evento y compra de entradas (otra issue de frontend).
      </p>
    </section>
  );
}
