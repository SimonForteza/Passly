import { useState, type FormEvent } from 'react';
import { ApiError } from '../lib/http/ApiError';
import { crearUsuario } from './api';
import { BotonPrimario, CampoTexto, estilos } from './componentes';
import type { Rol } from './types';

// Roles que solo un ADMIN puede dar de alta. COMPRADOR queda afuera: ese es el alta publica del
// registro. El backend impone la misma regla (@PreAuthorize en POST /api/usuarios).
const ROLES_PRIVILEGIADOS: Rol[] = ['ORGANIZADOR', 'VALIDADOR', 'ADMIN'];

export function AltaDeUsuarioAdmin() {
  const [nombre, setNombre] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [rol, setRol] = useState<Rol>('ORGANIZADOR');
  const [erroresCampo, setErroresCampo] = useState<Record<string, string>>({});
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null);
  const [exito, setExito] = useState<string | null>(null);
  const [cargando, setCargando] = useState(false);

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    setErroresCampo({});
    setErrorGeneral(null);
    setExito(null);
    setCargando(true);
    try {
      const creado = await crearUsuario({ email, nombre, password, rol });
      setExito(`Se creó ${creado.nombre} (${creado.rol}).`);
      setNombre('');
      setEmail('');
      setPassword('');
      setRol('ORGANIZADOR');
    } catch (e) {
      if (e instanceof ApiError && e.status === 400 && e.errores) {
        setErroresCampo(e.errores);
      } else if (e instanceof ApiError && e.status === 403) {
        setErrorGeneral('No tenés permiso para dar de alta este rol.');
      } else if (e instanceof ApiError) {
        setErrorGeneral(e.detail || e.title);
      } else {
        setErrorGeneral('No pudimos crear el usuario. Intentá de nuevo.');
      }
    } finally {
      setCargando(false);
    }
  }

  return (
    <div style={{ borderTop: '1px solid var(--color-border)', paddingTop: '2rem' }}>
      <h3>Alta de usuario (admin)</h3>
      <p style={{ color: 'var(--color-texto-secundario)', marginTop: '0.5rem' }}>
        Crear cuentas con rol privilegiado. El alta de compradores es pública, desde el registro.
      </p>

      <form onSubmit={enviar} style={{ ...estilos.formulario, marginTop: '1rem' }}>
        <CampoTexto
          etiqueta="Nombre"
          valor={nombre}
          onCambio={setNombre}
          requerido
          error={erroresCampo.nombre}
        />
        <CampoTexto
          etiqueta="Email"
          tipo="email"
          valor={email}
          onCambio={setEmail}
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

        <label style={estilos.campo}>
          <span style={estilos.etiqueta}>Rol</span>
          <select value={rol} onChange={(e) => setRol(e.target.value as Rol)} style={estilos.input}>
            {ROLES_PRIVILEGIADOS.map((r) => (
              <option key={r} value={r}>
                {r}
              </option>
            ))}
          </select>
          {erroresCampo.rol && <span style={estilos.errorCampo}>{erroresCampo.rol}</span>}
        </label>

        {errorGeneral && (
          <p
            role="alert"
            style={{
              color: 'var(--color-texto)',
              background: 'rgba(232, 18, 58, 0.12)',
              border: '1px solid var(--color-acento)',
              borderRadius: 'var(--radio-input)',
              padding: '0.6rem 0.85rem',
              margin: 0,
            }}
          >
            {errorGeneral}
          </p>
        )}

        {exito && (
          <p
            role="status"
            style={{
              color: 'var(--color-texto)',
              background: 'rgba(34, 197, 94, 0.12)',
              border: '1px solid var(--color-exito)',
              borderRadius: 'var(--radio-input)',
              padding: '0.6rem 0.85rem',
              margin: 0,
            }}
          >
            {exito}
          </p>
        )}

        <BotonPrimario cargando={cargando}>{cargando ? 'Creando…' : 'Crear usuario'}</BotonPrimario>
      </form>
    </div>
  );
}
