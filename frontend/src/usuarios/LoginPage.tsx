import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { ApiError } from '../lib/http/ApiError';
import { useAuth } from '../lib/auth/AuthContext';
import { obtenerUsuarioActual } from './api';
import { BotonPrimario, CampoTexto, estilos } from './componentes';

export function LoginPage() {
  const { iniciarSesion } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  // Si RutaPrivada nos mando aca, volvemos a la pantalla que se quiso abrir; si no, a la cartelera.
  const destino = (location.state as { from?: { pathname: string } } | null)?.from?.pathname ?? '/';

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [cargando, setCargando] = useState(false);

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    setError(null);
    setCargando(true);
    try {
      // No hay endpoint de "login": validamos las credenciales pidiendo el propio usuario. Si
      // sirven, el backend responde con el UsuarioDTO (rol incluido) y recien ahi damos la sesion
      // por buena; si no, /api/usuarios/me devuelve 401.
      const usuario = await obtenerUsuarioActual({ email, password });
      iniciarSesion(usuario, password);
      navigate(destino, { replace: true });
    } catch (e) {
      // 401 = credenciales invalidas (no hay caso de rol equivocado: /me solo pide estar
      // autenticado). Cualquier otra cosa es un error inesperado del backend o de red.
      if (e instanceof ApiError && e.status === 401) {
        setError('Email o contraseña inválidos.');
      } else {
        setError('No pudimos validar tus credenciales. Intentá de nuevo.');
      }
    } finally {
      setCargando(false);
    }
  }

  return (
    <section>
      <h2>Ingresar</h2>
      <form onSubmit={enviar} style={estilos.formulario}>
        <CampoTexto
          etiqueta="Email"
          tipo="email"
          valor={email}
          onCambio={setEmail}
          autoComplete="email"
          requerido
        />
        <CampoTexto
          etiqueta="Contraseña"
          tipo="password"
          valor={password}
          onCambio={setPassword}
          autoComplete="current-password"
          requerido
        />

        {error && (
          <p
            role="alert"
            style={{
              color: 'var(--color-texto)',
              background: 'rgba(232, 17, 45, 0.12)',
              border: '1px solid var(--color-acento)',
              borderRadius: 'var(--radio-input)',
              padding: '0.6rem 0.85rem',
              margin: 0,
            }}
          >
            {error}
          </p>
        )}

        <BotonPrimario cargando={cargando}>{cargando ? 'Ingresando…' : 'Ingresar'}</BotonPrimario>
      </form>

      <p style={{ color: 'var(--color-texto-secundario)', marginTop: '1.5rem' }}>
        ¿No tenés cuenta? <Link to="/registro">Crear una</Link>
      </p>
    </section>
  );
}
