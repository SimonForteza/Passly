import { useEffect, useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { EstadoCarga } from '../layout/EstadoCarga';
import { EstadoError } from '../layout/EstadoError';
import { useAuth } from '../lib/auth/AuthContext';
import { agregarMiembro, listarMiembros, obtenerProductora } from './api';
import type { MiembroDTO, ProductoraDTO, RolEnProductora } from './types';

export function BackofficePage() {
  const { id } = useParams();
  const { usuario } = useAuth();
  const idProductora = Number(id);
  const idValido = Number.isInteger(idProductora) && idProductora > 0;
  const [productora, setProductora] = useState<ProductoraDTO | null>(null);
  const [miembros, setMiembros] = useState<MiembroDTO[]>([]);
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [mensaje, setMensaje] = useState('');
  const [email, setEmail] = useState('');
  const [rol, setRol] = useState<RolEnProductora>('STAFF');

  useEffect(() => {
    let vigente = true;

    if (!idValido) return;

    Promise.all([obtenerProductora(idProductora), listarMiembros(idProductora)])
      .then(([detalle, padron]) => {
        if (!vigente) return;
        setProductora(detalle);
        setMiembros(padron);
      })
      .catch((causa: unknown) => {
        if (vigente) setError(causa);
      })
      .finally(() => {
        if (vigente) setCargando(false);
      });

    return () => {
      vigente = false;
    };
  }, [idProductora, idValido]);

  async function enviarMiembro(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    setGuardando(true);
    setError(null);
    setMensaje('');

    try {
      const nuevo = await agregarMiembro(idProductora, {
        email,
        rolEnProductora: rol,
      });
      setMiembros((actuales) => [...actuales, nuevo].sort((a, b) => a.nombre.localeCompare(b.nombre)));
      setEmail('');
      setRol('STAFF');
      setMensaje(`${nuevo.nombre} fue incorporado al padrón.`);
    } catch (causa: unknown) {
      setError(causa);
    } finally {
      setGuardando(false);
    }
  }

  const membresiaActual = miembros.find((miembro) => miembro.idUsuario === usuario?.id);
  const esDuenio = membresiaActual?.rolEnProductora === 'DUENIO';

  if (!idValido) {
    return (
      <section className="stack-grande">
        <Link className="volver" to="/productoras">← Mis productoras</Link>
        <p className="mensaje-error" role="alert">El identificador de la productora no es válido.</p>
      </section>
    );
  }

  if (cargando) return <EstadoCarga mensaje="Cargando el padrón..." />;

  return (
    <section className="stack-grande">
      <Link className="volver" to="/productoras">← Mis productoras</Link>

      {productora ? (
        <div className="encabezado-pagina">
          <div>
            <p className="eyebrow">Backoffice</p>
            <h2>{productora.nombreComercial}</h2>
            <p className="texto-secundario">
              {productora.descripcion || 'Gestión de integrantes de la productora.'}
            </p>
            {productora.cuit ? <p className="dato-fiscal">CUIT {productora.cuit}</p> : null}
          </div>
          {membresiaActual ? <span className="badge">Tu rol: {etiquetaRol(membresiaActual.rolEnProductora)}</span> : null}
        </div>
      ) : null}

      {error ? <EstadoError error={error} /> : null}
      {mensaje ? <p className="mensaje-exito" role="status">{mensaje}</p> : null}

      {productora ? (
        <div className="panel">
          <div className="titulo-seccion">
            <div>
              <p className="eyebrow">Equipo</p>
              <h3>Padrón de miembros</h3>
            </div>
            <span className="contador">{miembros.length} {miembros.length === 1 ? 'miembro' : 'miembros'}</span>
          </div>

          <div className="tabla-responsive">
            <table>
              <thead>
                <tr>
                  <th>Nombre</th>
                  <th>Email</th>
                  <th>Rol</th>
                </tr>
              </thead>
              <tbody>
                {miembros.map((miembro) => (
                  <tr key={miembro.idUsuario}>
                    <td>{miembro.nombre}</td>
                    <td>{miembro.email}</td>
                    <td><span className="badge">{etiquetaRol(miembro.rolEnProductora)}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      ) : null}

      {productora && esDuenio ? (
        <form className="panel formulario" onSubmit={enviarMiembro}>
          <div>
            <p className="eyebrow">Administración</p>
            <h3>Incorporar miembro</h3>
            <p className="texto-secundario">
              El usuario debe existir y su rol global debe ser compatible con el rol asignado.
            </p>
          </div>

          <label>
            Email del usuario
            <input
              required
              type="email"
              value={email}
              onChange={(evento) => setEmail(evento.target.value)}
            />
          </label>

          <label>
            Rol dentro de la productora
            <select value={rol} onChange={(evento) => setRol(evento.target.value as RolEnProductora)}>
              <option value="STAFF">Staff</option>
              <option value="VALIDADOR">Validador</option>
              <option value="DUENIO">Dueño</option>
            </select>
          </label>

          <button className="boton boton-primario" type="submit" disabled={guardando}>
            {guardando ? 'Incorporando...' : 'Incorporar miembro'}
          </button>
        </form>
      ) : null}
    </section>
  );
}

function etiquetaRol(rol: RolEnProductora): string {
  const etiquetas: Record<RolEnProductora, string> = {
    DUENIO: 'Dueño',
    STAFF: 'Staff',
    VALIDADOR: 'Validador',
  };
  return etiquetas[rol];
}
