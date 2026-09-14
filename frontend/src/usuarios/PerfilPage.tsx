import { useAuth } from '../lib/auth/AuthContext';
import { AltaDeUsuarioAdmin } from './AltaDeUsuarioAdmin';

export function PerfilPage() {
  const { usuario } = useAuth();

  // RutaPrivada garantiza que hay usuario; este chequeo es solo para estrechar el tipo.
  if (!usuario) return null;

  return (
    <section style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
      <div>
        <h2>Mi perfil</h2>
        <dl style={{ display: 'grid', gridTemplateColumns: 'auto 1fr', gap: '0.5rem 1.25rem', marginTop: '1rem' }}>
          <Dato etiqueta="Nombre" valor={usuario.nombre} />
          <Dato etiqueta="Email" valor={usuario.email} />
          <Dato etiqueta="Rol" valor={usuario.rol} />
        </dl>
      </div>

      {/* Bonus: solo un ADMIN ve y opera el alta de roles privilegiados. */}
      {usuario.rol === 'ADMIN' && <AltaDeUsuarioAdmin />}
    </section>
  );
}

function Dato({ etiqueta, valor }: { etiqueta: string; valor: string }) {
  return (
    <>
      <dt style={{ color: 'var(--color-texto-secundario)' }}>{etiqueta}</dt>
      <dd style={{ margin: 0 }}>{valor}</dd>
    </>
  );
}
