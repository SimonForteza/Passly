import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../lib/auth/AuthContext';
import { useCambiosSinGuardar } from '../lib/useCambiosSinGuardar';
import { EstadoError } from '../layout/EstadoError';
import { crearProductora } from './api';
import { useOrganizaciones } from './OrganizacionContext';
export function CrearProductoraPage() {
  const navigate = useNavigate();
  const { actualizarUsuario } = useAuth();
  const { recargar } = useOrganizaciones();
  const [nombre, setNombre] = useState('');
  const [cuit, setCuit] = useState('');
  const [descripcion, setDescripcion] = useState('');
  const [logo, setLogo] = useState('');
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState<unknown>(null);
  useCambiosSinGuardar(!!(nombre || cuit || descripcion || logo) && !guardando);
  async function enviar(e: FormEvent) {
    e.preventDefault();
    setGuardando(true);
    setError(null);
    try {
      const p = await crearProductora({
        nombreComercial: nombre.trim(),
        cuit: cuit.trim() || undefined,
        descripcion: descripcion.trim() || undefined,
        logoUrl: logo.trim() || undefined,
      });
      await actualizarUsuario();
      await recargar();
      navigate('/productoras/' + p.id + '/gestion', {
        replace: true,
        state: { productoraCreada: true },
      });
    } catch (causa) {
      setError(causa);
      setGuardando(false);
    }
  }
  return (
    <section className="stack-grande">
      <Link className="volver" to="/productoras">
        ← Panel de organización
      </Link>
      <div>
        <p className="eyebrow">Tu organización</p>
        <h2>Crear productora</h2>
        <p>
          Con un nombre comercial ya podés empezar. Los demás datos son
          opcionales.
        </p>
      </div>
      {error ? <EstadoError error={error} /> : null}
      <form className="panel formulario" onSubmit={enviar}>
        <label>
          Nombre comercial
          <input
            required
            maxLength={150}
            autoComplete="organization"
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
          />
        </label>
        <details>
          <summary>Datos opcionales</summary>
          <div className="stack-chico datos-opcionales">
            <label>
              CUIT{' '}
              <span className="ayuda-campo">(11 dígitos, sin guiones)</span>
              <input
                inputMode="numeric"
                pattern="[0-9]{11}"
                maxLength={11}
                value={cuit}
                onChange={(e) => setCuit(e.target.value)}
              />
            </label>
            <label>
              Descripción
              <textarea
                rows={3}
                maxLength={2000}
                value={descripcion}
                onChange={(e) => setDescripcion(e.target.value)}
              />
            </label>
            <label>
              Enlace a tu logo
              <input
                type="url"
                maxLength={500}
                placeholder="https://…"
                value={logo}
                onChange={(e) => setLogo(e.target.value)}
              />
              <span className="ayuda-campo">
                Usá el enlace público de una imagen que ya esté alojada en
                internet.
              </span>
            </label>
            {logo ? (
              <img
                className="vista-logo"
                src={logo}
                alt="Vista previa del logo"
              />
            ) : null}
          </div>
        </details>
        <button className="boton boton-primario" disabled={guardando}>
          {guardando ? 'Creando productora…' : 'Crear productora'}
        </button>
      </form>
    </section>
  );
}
