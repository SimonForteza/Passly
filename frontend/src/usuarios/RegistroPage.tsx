import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { ApiError } from '../lib/http/ApiError';
import { useAuth } from '../lib/auth/AuthContext';
import { crearUsuario } from './api';
import {
  BotonPrimario,
  CampoTexto,
  PantallaAuth,
  estilos,
} from './componentes';

export function RegistroPage() {
  const { iniciarSesion } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  // Mismo criterio que LoginPage: quien llegó por el link de un evento (Comprar -> login ->
  // "Crear una") trae ese evento en state.from y vuelve a él después del alta (PAS-25).
  const origen = (
    location.state as {
      from?: { pathname: string; search?: string; hash?: string };
    } | null
  )?.from;
  const contexto =
    (location.state as { contexto?: string } | null)?.contexto ??
    (origen?.pathname === '/productoras/nueva'
      ? 'Primero creá tu cuenta. Después continuás con el alta de tu productora.'
      : '');
  const destino =
    origen?.pathname?.startsWith('/') && !origen.pathname.startsWith('//')
      ? `${origen.pathname}${origen.search ?? ''}${origen.hash ?? ''}`
      : '/';

  const [nombre, setNombre] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  // Errores por campo del 400 de validacion (mapa `errores` del ProblemDetail) y un error general
  // para el resto (email ya registrado, red, etc.).
  const [erroresCampo, setErroresCampo] = useState<Record<string, string>>({});
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null);
  const [cargando, setCargando] = useState(false);

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    setErroresCampo({});
    setErrorGeneral(null);
    setCargando(true);
    try {
      // El alta publica SIEMPRE crea COMPRADOR: no ofrecemos elegir rol. Pedir uno privilegiado
      // como anonimo es 401 en el backend; el alta de otros roles la hace un ADMIN desde su perfil.
      const usuario = await crearUsuario({
        email,
        nombre,
        password,
        rol: 'COMPRADOR',
      });
      // El POST devuelve el UsuarioDTO (con id): con eso y la clave ya podemos dejar la sesion
      // iniciada sin pasar por el login.
      iniciarSesion(usuario, password);
      navigate(destino, { replace: true });
    } catch (e) {
      if (e instanceof ApiError && e.status === 400 && e.errores) {
        setErroresCampo(e.errores);
      } else if (e instanceof ApiError) {
        setErrorGeneral(e.detail || e.title);
      } else {
        setErrorGeneral('No pudimos crear la cuenta. Intentá de nuevo.');
      }
    } finally {
      setCargando(false);
    }
  }

  return (
    <PantallaAuth>
      <h2>Crear cuenta</h2>
      {contexto ? <p className="texto-secundario">{contexto}</p> : null}
      <form onSubmit={enviar} style={estilos.formulario}>
        <CampoTexto
          etiqueta="Nombre"
          valor={nombre}
          onCambio={setNombre}
          autoComplete="name"
          requerido
          error={erroresCampo.nombre}
        />
        <CampoTexto
          etiqueta="Email"
          tipo="email"
          valor={email}
          onCambio={setEmail}
          autoComplete="email"
          requerido
          error={erroresCampo.email}
        />
        <CampoTexto
          etiqueta="Contraseña"
          tipo="password"
          valor={password}
          onCambio={setPassword}
          autoComplete="new-password"
          requerido
          error={erroresCampo.password}
        />

        {errorGeneral && (
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
            {errorGeneral}
          </p>
        )}

        <BotonPrimario cargando={cargando}>
          {cargando ? 'Creando…' : 'Crear cuenta'}
        </BotonPrimario>
      </form>

      <p
        style={{ color: 'var(--color-texto-secundario)', marginTop: '1.5rem' }}
      >
        ¿Ya tenés cuenta?{' '}
        <Link to="/login" state={location.state}>
          Ingresar
        </Link>
      </p>
    </PantallaAuth>
  );
}
