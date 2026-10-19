import { Link } from 'react-router-dom';
import { EstadoCarga } from '../layout/EstadoCarga';
import { EstadoError } from '../layout/EstadoError';
import { useOrganizaciones } from './OrganizacionContext';
export function ProductorasPage() {
  const { organizaciones, cargando, error, recargar } = useOrganizaciones();
  return (
    <section className="stack-grande">
      <div className="encabezado-pagina">
        <div>
          <p className="eyebrow">Organización</p>
          <h2>Mis productoras</h2>
          <p className="texto-secundario">
            Elegí la productora con la que querés trabajar.
          </p>
        </div>
        {organizaciones.length ? (
          <Link className="boton boton-secundario" to="/productoras/nueva">
            Crear otra productora
          </Link>
        ) : null}
      </div>
      {error ? (
        <>
          <EstadoError error={error} />
          <button
            className="boton boton-secundario"
            onClick={() => {
              void recargar().catch(() => undefined);
            }}
          >
            Volver a cargar
          </button>
        </>
      ) : null}
      {cargando ? (
        <EstadoCarga mensaje="Cargando tus productoras…" />
      ) : !error && !organizaciones.length ? (
        <div className="panel estado-vacio">
          <h3>Empezá con tu primera productora</h3>
          <p>
            También podés sumarte a un equipo aceptando una invitación por
            email.
          </p>
          <Link className="boton boton-primario" to="/productoras/nueva">
            Crear mi productora
          </Link>
        </div>
      ) : (
        <div className="grilla-tarjetas">
          {organizaciones.map((p) => (
            <article className="panel tarjeta-productora" key={p.id}>
              {p.logoUrl ? (
                <img src={p.logoUrl} alt="" />
              ) : (
                <div className="logo-placeholder" aria-hidden="true">
                  {p.nombreComercial.slice(0, 1)}
                </div>
              )}
              <h3>{p.nombreComercial}</h3>
              <span className="badge">
                {p.rol === 'DUENIO'
                  ? 'Dueño'
                  : p.rol === 'STAFF'
                    ? 'Staff'
                    : 'Validador'}
              </span>
              {p.descripcion ? (
                <p className="texto-secundario">{p.descripcion}</p>
              ) : null}
              <Link
                className="boton boton-primario"
                to={
                  p.puedeGestionar
                    ? '/productoras/' + p.id + '/gestion'
                    : '/validar?productora=' + p.id
                }
              >
                {p.puedeGestionar ? 'Abrir panel' : 'Validar entradas'}
              </Link>
              <Link
                className="volver"
                to={'/productoras/' + p.id + '/backoffice'}
              >
                Ver equipo
              </Link>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
