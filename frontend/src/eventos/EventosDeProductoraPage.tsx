import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { EstadoError } from '../layout/EstadoError';
import { EstadoCarga } from '../layout/EstadoCarga';
import { useProductoraActual } from '../productoras/GestionLayout';
import { formatearFechaEvento } from '../ventas/formato';
import { listarEventosDeProductora } from './api';
import { PublicarEvento } from './PublicarEvento';
import type { EventoDTO } from './types';
export function EventosDeProductoraPage() {
  const p = useProductoraActual();
  const aceptada = (
    useLocation().state as { invitacionAceptada?: boolean } | null
  )?.invitacionAceptada;
  const [eventos, setEventos] = useState<EventoDTO[] | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [mensaje, setMensaje] = useState('');
  useEffect(() => {
    let vigente = true;
    listarEventosDeProductora(p.id)
      .then((v) => {
        if (vigente) setEventos(v);
      })
      .catch((e) => {
        if (vigente) setError(e);
      });
    return () => {
      vigente = false;
    };
  }, [p.id]);
  const base = '/productoras/' + p.id + '/eventos';
  return (
    <section className="stack-grande">
      {aceptada ? (
        <p className="mensaje-exito" role="status">
          Invitación aceptada. Ya podés gestionar los eventos de esta
          productora.
        </p>
      ) : null}
      <div className="encabezado-pagina">
        <div>
          <p className="eyebrow">{p.nombreComercial}</p>
          <h2>Eventos</h2>
          <p className="texto-secundario">
            Prepará tus borradores y gestioná los eventos publicados.
          </p>
        </div>
        {eventos?.length ? (
          <Link className="boton boton-primario" to={base + '/nuevo'}>
            Crear evento
          </Link>
        ) : null}
      </div>
      {mensaje ? (
        <p className="mensaje-exito" role="status">
          {mensaje}
        </p>
      ) : null}
      {error ? (
        <EstadoError error={error} />
      ) : !eventos ? (
        <EstadoCarga mensaje="Cargando tus eventos…" />
      ) : !eventos.length ? (
        <div className="panel estado-vacio">
          <h3>Prepará tu primer evento</h3>
          <p>Podés guardarlo como borrador y publicarlo cuando esté listo.</p>
          <Link className="boton boton-primario" to={base + '/nuevo'}>
            Crear mi primer evento
          </Link>
        </div>
      ) : (
        <div className="lista-eventos-gestion">
          {eventos.map((e) => (
            <article className="panel evento-gestion" key={e.id}>
              <div className="evento-gestion-info">
                <span className={'badge badge-' + e.estado.toLowerCase()}>
                  {e.estado === 'BORRADOR'
                    ? 'Borrador'
                    : e.estado === 'PUBLICADO'
                      ? 'Publicado'
                      : 'Cancelado'}
                </span>
                <h3>
                  <Link to={base + '/' + e.id}>{e.nombre}</Link>
                </h3>
                <p>
                  {formatearFechaEvento(e.fechaHora)} · {e.lugar}
                </p>
              </div>
              <div className="acciones">
                <Link className="boton boton-secundario" to={base + '/' + e.id}>
                  Gestionar evento
                </Link>
                {e.estado === 'BORRADOR' ? (
                  <PublicarEvento
                    evento={e}
                    onPublicado={(v) => {
                      setEventos((actuales) =>
                        actuales!.map((x) => (x.id === v.id ? v : x)),
                      );
                      setMensaje(v.nombre + ' ya está publicado.');
                    }}
                  />
                ) : null}
                {e.estado === 'PUBLICADO' ? (
                  <Link
                    className="volver"
                    to={'/eventos/' + e.id + '?vistaPrevia=1'}
                  >
                    Ver página pública ↗
                  </Link>
                ) : null}
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
