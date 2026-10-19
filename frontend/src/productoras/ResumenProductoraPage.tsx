import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { listarEventosDeProductora } from '../eventos/api';
import type { EventoDTO } from '../eventos/types';
import { EstadoError } from '../layout/EstadoError';
import { useProductoraActual } from './GestionLayout';
export function ResumenProductoraPage() {
  const p = useProductoraActual();
  const location = useLocation();
  const [eventos, setEventos] = useState<EventoDTO[] | null>(null);
  const [error, setError] = useState<unknown>(null);
  useEffect(() => {
    let vigente = true;
    if (p.puedeGestionar)
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
  }, [p.id, p.puedeGestionar]);
  const nueva = (location.state as { productoraCreada?: boolean } | null)
    ?.productoraCreada;
  const base = '/productoras/' + p.id;
  return (
    <section className="stack-grande">
      {nueva ? (
        <p className="mensaje-exito" role="status">
          Tu productora está lista. Ya podés preparar tu primer evento.
        </p>
      ) : null}
      <div>
        <p className="eyebrow">Resumen</p>
        <h2>{p.nombreComercial}</h2>
        {p.descripcion ? (
          <p className="texto-secundario">{p.descripcion}</p>
        ) : null}
      </div>
      {error ? <EstadoError error={error} /> : null}
      {p.puedeGestionar ? (
        <>
          {eventos ? (
            <dl className="metricas">
              <div>
                <dt>Eventos</dt>
                <dd>{eventos.length}</dd>
              </div>
              <div>
                <dt>Borradores</dt>
                <dd>{eventos.filter((e) => e.estado === 'BORRADOR').length}</dd>
              </div>
              <div>
                <dt>Publicados</dt>
                <dd>
                  {eventos.filter((e) => e.estado === 'PUBLICADO').length}
                </dd>
              </div>
            </dl>
          ) : null}
          <div className="panel stack-chico">
            <h3>
              {eventos?.length
                ? 'Seguí trabajando en tus eventos'
                : 'Prepará tu primer evento'}
            </h3>
            <p>
              Guardá un borrador, cargá sus entradas y revisá todo antes de
              publicarlo.
            </p>
            <div className="acciones">
              <Link
                className="boton boton-primario"
                to={base + '/eventos/nuevo'}
              >
                {eventos?.length ? 'Crear evento' : 'Crear mi primer evento'}
              </Link>
              {eventos?.length ? (
                <Link className="boton boton-secundario" to={base + '/eventos'}>
                  Ver mis eventos
                </Link>
              ) : null}
            </div>
          </div>
        </>
      ) : (
        <div className="panel stack-chico">
          <h3>Controlá el ingreso a tus eventos</h3>
          <p>Elegí un evento publicado de esta productora para empezar.</p>
          <Link
            className="boton boton-primario"
            to={'/validar?productora=' + p.id}
          >
            Validar entradas
          </Link>
        </div>
      )}
      {p.puedeInvitar ? (
        <div className="panel stack-chico">
          <h3>Sumá a tu equipo</h3>
          <p>
            Invitá staff para gestionar eventos o validadores para controlar el
            ingreso.
          </p>
          <div>
            <Link className="boton boton-secundario" to={base + '/backoffice'}>
              Invitar a mi equipo
            </Link>
          </div>
        </div>
      ) : null}
    </section>
  );
}
