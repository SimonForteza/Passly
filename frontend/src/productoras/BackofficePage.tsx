import { useParams } from 'react-router-dom';

export function BackofficePage() {
  const { id } = useParams();

  return (
    <section>
      <h2>Backoffice de productora #{id}</h2>
      <p style={{ color: 'var(--color-texto-secundario)' }}>
        Próximamente: administración de eventos y reportes (otra issue de frontend).
      </p>
    </section>
  );
}
