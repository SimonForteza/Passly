import { Fragment, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { EstadoCarga } from '../layout/EstadoCarga';
import { EstadoError } from '../layout/EstadoError';
import { obtenerProductora } from '../productoras/api';
import type { ProductoraDTO } from '../productoras/types';
import { listarEventosDeProductora, publicarEvento } from './api';
import type { EstadoEvento, EventoDTO } from './types';

const FORMATO_FECHA = new Intl.DateTimeFormat('es-AR', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
});

function claseBadgeEstado(estado: EstadoEvento): string {
  if (estado === 'PUBLICADO') return 'badge badge-publicado';
  if (estado === 'BORRADOR') return 'badge badge-borrador';
  return 'badge';
}

function linkDeEvento(idEvento: number): string {
  return `${window.location.origin}/eventos/${idEvento}`;
}

export function EventosDeProductoraPage() {
  const { id } = useParams();
  const idProductora = Number(id);
  const idValido = Number.isInteger(idProductora) && idProductora > 0;
  const [productora, setProductora] = useState<ProductoraDTO | null>(null);
  const [eventos, setEventos] = useState<EventoDTO[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<unknown>(null);
  const [publicando, setPublicando] = useState<number | null>(null);
  const [erroresPorFila, setErroresPorFila] = useState<Record<number, unknown>>({});
  // Fila cuyo link se acaba de copiar (o falló al copiarse), para la confirmación breve.
  const [copiado, setCopiado] = useState<{ idEvento: number; ok: boolean } | null>(null);

  useEffect(() => {
    let vigente = true;

    if (!idValido) return;

    Promise.all([obtenerProductora(idProductora), listarEventosDeProductora(idProductora)])
      .then(([detalle, lista]) => {
        if (!vigente) return;
        setProductora(detalle);
        setEventos(lista);
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

  async function publicar(idEvento: number) {
    setPublicando(idEvento);
    setErroresPorFila((actuales) => {
      if (!(idEvento in actuales)) return actuales;
      const copia = { ...actuales };
      delete copia[idEvento];
      return copia;
    });

    try {
      const actualizado = await publicarEvento(idEvento);
      setEventos((actuales) => actuales.map((evento) => (evento.id === idEvento ? actualizado : evento)));
    } catch (causa: unknown) {
      setErroresPorFila((actuales) => ({ ...actuales, [idEvento]: causa }));
    } finally {
      setPublicando(null);
    }
  }

  // El link compartible es el detalle público del evento (PAS-25): lo abre cualquiera, con o sin
  // sesión. navigator.clipboard solo existe en contextos seguros (https o localhost); si falla, la
  // fila muestra el link para copiarlo a mano.
  async function copiarLink(idEvento: number) {
    let ok = true;
    try {
      await navigator.clipboard.writeText(linkDeEvento(idEvento));
    } catch {
      ok = false;
    }
    setCopiado({ idEvento, ok });
    if (ok) {
      window.setTimeout(() => {
        setCopiado((actual) => (actual?.idEvento === idEvento ? null : actual));
      }, 2000);
    }
  }

  if (!idValido) {
    return (
      <section className="stack-grande">
        <Link className="volver" to="/productoras">← Mis productoras</Link>
        <p className="mensaje-error" role="alert">El identificador de la productora no es válido.</p>
      </section>
    );
  }

  if (cargando) return <EstadoCarga mensaje="Cargando los eventos..." />;

  return (
    <section className="stack-grande">
      <Link className="volver" to="/productoras">← Mis productoras</Link>

      {productora ? (
        <div className="encabezado-pagina">
          <div>
            <p className="eyebrow">Backoffice</p>
            <h2>{productora.nombreComercial}</h2>
            <p className="texto-secundario">Eventos propios, incluidos los borradores.</p>
          </div>
          <div className="acciones-fila">
            {/* Lo que ve el público: solo los publicados, sin los borradores de esta tabla (PAS-26). */}
            <Link className="boton boton-secundario" to={`/productoras/${idProductora}`}>
              Ver página pública
            </Link>
            <Link className="boton boton-primario" to={`/productoras/${idProductora}/eventos/nuevo`}>
              Crear evento
            </Link>
          </div>
        </div>
      ) : null}

      {error ? <EstadoError error={error} /> : null}

      {productora && eventos.length === 0 ? (
        <div className="panel estado-vacio">
          <h3>Todavía no cargaste ningún evento</h3>
          <p className="texto-secundario">Creá el primero con el botón de arriba.</p>
        </div>
      ) : null}

      {productora && eventos.length > 0 ? (
        <div className="panel">
          <div className="tabla-responsive">
            <table>
              <thead>
                <tr>
                  <th>Nombre</th>
                  <th>Fecha</th>
                  <th>Lugar</th>
                  <th>Estado</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {eventos.map((evento) => (
                  <Fragment key={evento.id}>
                    <tr>
                      <td>
                        <Link to={`/eventos/${evento.id}`}>{evento.nombre}</Link>
                      </td>
                      <td>{FORMATO_FECHA.format(new Date(evento.fechaHora))}</td>
                      <td>{evento.lugar}</td>
                      <td><span className={claseBadgeEstado(evento.estado)}>{evento.estado}</span></td>
                      <td>
                        <div className="acciones-fila">
                          {evento.estado !== 'CANCELADO' ? (
                            <Link
                              className="boton boton-secundario"
                              to={`/productoras/${idProductora}/eventos/${evento.id}/editar`}
                            >
                              Editar
                            </Link>
                          ) : null}
                          {evento.estado === 'BORRADOR' ? (
                            <button
                              className="boton boton-secundario"
                              type="button"
                              onClick={() => publicar(evento.id)}
                              disabled={publicando === evento.id}
                            >
                              {publicando === evento.id ? 'Publicando...' : 'Publicar'}
                            </button>
                          ) : null}
                          {evento.estado === 'PUBLICADO' ? (
                            <button
                              className="boton boton-secundario"
                              type="button"
                              onClick={() => copiarLink(evento.id)}
                            >
                              {copiado?.idEvento === evento.id && copiado.ok ? '¡Link copiado!' : 'Copiar link'}
                            </button>
                          ) : null}
                          {evento.estado === 'BORRADOR' ? (
                            // Un botón deshabilitado no dispara eventos de mouse en todos los
                            // navegadores: el tooltip va en el span que lo envuelve.
                            <span title="Publicá el evento para compartirlo">
                              <button className="boton boton-secundario" type="button" disabled>
                                Copiar link
                              </button>
                            </span>
                          ) : null}
                        </div>
                      </td>
                    </tr>
                    {copiado?.idEvento === evento.id && !copiado.ok ? (
                      <tr>
                        <td colSpan={5}>
                          <p className="texto-secundario" role="status">
                            No pudimos copiarlo automáticamente. El link es: {linkDeEvento(evento.id)}
                          </p>
                        </td>
                      </tr>
                    ) : null}
                    {erroresPorFila[evento.id] ? (
                      <tr>
                        <td colSpan={5}>
                          <EstadoError error={erroresPorFila[evento.id]} />
                        </td>
                      </tr>
                    ) : null}
                  </Fragment>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      ) : null}
    </section>
  );
}
