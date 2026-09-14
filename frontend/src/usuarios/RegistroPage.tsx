import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ApiError } from '../lib/http/ApiError';
import { useAuth } from '../lib/auth/AuthContext';
import { crearUsuario } from './api';
import { BotonPrimario, CampoTexto, estilos } from './componentes';

export function RegistroPage() {
  const { iniciarSesion } = useAuth();
  const navigate = useNavigate();

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
      const usuario = await crearUsuario({ email, nombre, password, rol: 'COMPRADOR' });
      // El POST devuelve el UsuarioDTO (con id): con eso y la clave ya podemos dejar la sesion
      // iniciada sin pasar por el login.
      iniciarSesion(usuario, password);
      navigate('/', { replace: true });
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
    <section>
      <h2>Crear cuenta</h2>
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

        <BotonPrimario cargando={cargando}>{cargando ? 'Creando…' : 'Crear cuenta'}</BotonPrimario>
      </form>

      <p style={{ color: 'var(--color-texto-secundario)', marginTop: '1.5rem' }}>
        ¿Ya tenés cuenta? <Link to="/login">Ingresar</Link>
      </p>
    </section>
  );
}
