import { useEffect, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { EstadoCarga } from '../layout/EstadoCarga';
import { EstadoError } from '../layout/EstadoError';
import { useProductoraActual } from '../productoras/GestionLayout';
import { formatearFechaEvento, formatearPrecio } from '../ventas/formato';
import { obtenerEvento } from './api';
import { PublicarEvento } from './PublicarEvento';
import type { EventoDTO } from './types';
export function GestionEventoPage() {
  const p = useProductoraActual();
  const { idEvento } = useParams();
  const location = useLocation();
  const [evento, setEvento] = useState<EventoDTO | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [mensaje, setMensaje] = useState('');
  useEffect(() => {
    let vigente = true;
    obtenerEvento(Number(idEvento))
      .then((e) => {
        if (!vigente) return;
        if (e.organizador.id !== p.id) {
          setError(new Error('Productora incorrecta'));
          return;
        }
        setEvento(e);
      })
      .catch((e) => {
        if (vigente) setError(e);
      });
    return () => {
      vigente = false;
    };
  }, [idEvento, p.id]);
  if (error) return <EstadoError error={error} />;
  if (!evento) return <EstadoCarga mensaje="Cargando tu evento…" />;
  const base = '/productoras/' + p.id + '/eventos';
  const creado = (location.state as { eventoCreado?: boolean } | null)
    ?.eventoCreado;
  async function copiar() {
    try {
      await navigator.clipboard.writeText(
        window.location.origin + '/eventos/' + evento!.id,
      );
      setMensaje('Enlace público copiado. Ya podés compartirlo.');
    } catch {
      setMensaje(
        'Copiá este enlace: ' +
          window.location.origin +
          '/eventos/' +
          evento!.id,
      );
    }
  }
  return (
    <section className="stack-grande">
      <Link className="volver" to={base}>
        ← Eventos de {p.nombreComercial}
      </Link>
      {creado && evento.estado === 'BORRADOR' ? (
        <p className="mensaje-exito" role="status">
          Tu evento se guardó como borrador. Revisalo antes de publicarlo.
        </p>
      ) : null}
      {mensaje ? (
        <p className="mensaje-exito" role="status">
          {mensaje}
        </p>
      ) : null}
      <div className="encabezado-pagina">
        <div>
          <p className="eyebrow">Gestión del evento</p>
          <h2>{evento.nombre}</h2>
          <p>
            {formatearFechaEvento(evento.fechaHora)} · {evento.lugar}
          </p>
        </div>
        <span className={'badge badge-' + evento.estado.toLowerCase()}>
          {evento.estado === 'BORRADOR'
            ? 'Borrador'
            : evento.estado === 'PUBLICADO'
              ? 'Publicado'
              : 'Cancelado'}
        </span>
      </div>
      <div className="acciones">
        {evento.estado !== 'CANCELADO' ? (
          <Link
            className="boton boton-secundario"
            to={base + '/' + evento.id + '/editar'}
          >
            Editar evento
          </Link>
        ) : null}
        <Link
          className="boton boton-secundario"
          to={'/eventos/' + evento.id + '?vistaPrevia=1'}
        >
          Vista previa
        </Link>
        {evento.estado === 'BORRADOR' ? (
          <PublicarEvento
            evento={evento}
            onPublicado={(e) => {
              setEvento(e);
              setMensaje('Evento publicado. Sus entradas ya están a la venta.');
            }}
          />
        ) : null}
        {evento.estado === 'PUBLICADO' ? (
          <button className="boton boton-primario" onClick={copiar}>
            Copiar enlace público
          </button>
        ) : null}
      </div>
      {evento.descripcion ? <p>{evento.descripcion}</p> : null}
      <div className="panel stack-chico">
        <h3>Entradas de este evento</h3>
        <ul className="lista-resumen">
          {evento.tiposEntrada.map((t) => (
            <li key={t.id}>
              <strong>{t.nombre}</strong>
              <span>
                {formatearPrecio(t.precio)} · {t.cupoDisponible} disponibles de{' '}
                {t.cupoTotal}
              </span>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
