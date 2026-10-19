import { useEffect, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { EstadoCarga } from '../layout/EstadoCarga';
import { EstadoError } from '../layout/EstadoError';
import { obtenerOrden, obtenerTicketsDeOrden } from './api';
import { formatearFechaOrden, formatearPrecio } from './formato';
import { EntradasDeOrden } from './EntradasDeOrden';
import type { OrdenDTO, TicketDTO } from './types';
export function DetalleOrdenPage() {
  const { id } = useParams();
  const location = useLocation();
  const recienConfirmada = (
    location.state as { recienConfirmada?: boolean } | null
  )?.recienConfirmada;
  const [datos, setDatos] = useState<{
    orden: OrdenDTO;
    tickets: TicketDTO[];
  } | null>(null);
  const [error, setError] = useState<unknown>(null);
  useEffect(() => {
    let vigente = true;
    Promise.all([obtenerOrden(Number(id)), obtenerTicketsDeOrden(Number(id))])
      .then(([orden, tickets]) => {
        if (vigente) setDatos({ orden, tickets });
      })
      .catch((e) => {
        if (vigente) setError(e);
      });
    return () => {
      vigente = false;
    };
  }, [id]);
  if (error)
    return (
      <section className="stack-grande">
        <EstadoError error={error} />
        <Link to="/mis-ordenes">Volver a mis entradas</Link>
      </section>
    );
  if (!datos) return <EstadoCarga mensaje="Cargando tus entradas…" />;
  return (
    <section className="stack-grande">
      <Link className="volver" to="/mis-ordenes">
        ← Mis entradas
      </Link>
      <div>
        <p className="eyebrow">Orden #{datos.orden.id}</p>
        <h2>{recienConfirmada ? 'Compra confirmada' : 'Tus entradas'}</h2>
      </div>
      {recienConfirmada ? (
        <p className="mensaje-exito" role="status">
          Tus entradas ya están listas. Mostrá los QR al ingresar.
        </p>
      ) : null}
      <p>
        A nombre de {datos.orden.comprador.nombre} ·{' '}
        {datos.orden.comprador.email}
      </p>
      <p>
        {formatearFechaOrden(datos.orden.creadaEn)} · Total{' '}
        {formatearPrecio(datos.orden.total)}
      </p>
      <EntradasDeOrden orden={datos.orden} tickets={datos.tickets} />
      <div className="acciones">
        <Link className="boton boton-secundario" to="/">
          Ver otros eventos
        </Link>
        <Link className="volver" to="/mis-ordenes">
          Todas mis entradas
        </Link>
      </div>
    </section>
  );
}
