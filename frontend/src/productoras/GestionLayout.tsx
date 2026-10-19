import {
  Link,
  NavLink,
  Outlet,
  useLocation,
  useNavigate,
  useOutletContext,
  useParams,
} from 'react-router-dom';
import { EstadoCarga } from '../layout/EstadoCarga';
import { EstadoError } from '../layout/EstadoError';
import { useOrganizaciones, type Organizacion } from './OrganizacionContext';

export function GestionLayout() {
  const { id } = useParams();
  const { organizaciones, cargando, error } = useOrganizaciones();
  const navigate = useNavigate();
  const location = useLocation();
  const productora = organizaciones.find((p) => p.id === Number(id));
  if (cargando) return <EstadoCarga mensaje="Cargando tu productora…" />;
  if (error) return <EstadoError error={error} />;
  if (!productora)
    return (
      <section className="stack-grande">
        <h2>Esta productora no está en tu equipo</h2>
        <p>Ingresá con una cuenta que forme parte de la productora.</p>
        <Link to="/productoras">Volver al panel de organización</Link>
      </section>
    );
  const requiereGestion = location.pathname.includes('/eventos');
  if (requiereGestion && !productora.puedeGestionar)
    return (
      <section className="stack-grande">
        <h2>Tu tarea es validar entradas</h2>
        <p>
          La gestión de eventos corresponde al dueño y al staff de esta
          productora.
        </p>
        <Link
          className="boton boton-primario"
          to={'/validar?productora=' + productora.id}
        >
          Ir a validar entradas
        </Link>
      </section>
    );
  const base = '/productoras/' + productora.id;
  return (
    <div className="gestion">
      <div className="gestion-contexto">
        <label>
          Productora
          <select
            aria-label="Cambiar productora"
            value={productora.id}
            onChange={(e) =>
              navigate('/productoras/' + e.target.value + '/gestion')
            }
          >
            {organizaciones.map((p) => (
              <option key={p.id} value={p.id}>
                {p.nombreComercial}
              </option>
            ))}
          </select>
        </label>
        <Link className="volver" to="/productoras">
          Todas mis productoras
        </Link>
      </div>
      <nav className="gestion-nav" aria-label="Secciones de la productora">
        <NavLink to={base + '/gestion'}>Resumen</NavLink>
        {productora.puedeGestionar ? (
          <NavLink to={base + '/eventos'}>Eventos</NavLink>
        ) : null}
        <NavLink to={base + '/backoffice'}>Equipo</NavLink>
        <Link to={'/validar?productora=' + productora.id}>
          Validar entradas
        </Link>
        <Link className="vista-publica" to={base}>
          Ver página pública ↗
        </Link>
      </nav>
      <Outlet context={productora} />
    </div>
  );
}
export function useProductoraActual() {
  return useOutletContext<Organizacion>();
}
