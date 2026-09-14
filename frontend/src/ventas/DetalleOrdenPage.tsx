import { useEffect, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { EstadoCarga } from '../layout/EstadoCarga';
import { EstadoError } from '../layout/EstadoError';
import { obtenerOrden, obtenerTicketsDeOrden } from './api';
import { formatearFechaEvento, formatearFechaOrden, formatearPrecio, pluralEntradas } from './formato';
import iconoCheckOk from './iconos/check-ok.svg';
import type { OrdenDTO, TicketDTO } from './types';
import { nombreDeEvento, useEventos } from './useEventos';

/**
 * Compra confirmada / detalle de una orden (Figma Web 04).
 *
 * Una tarjeta por ticket individual emitido por ServicioDeTickets. Una linea de orden con
 * cantidad mayor a uno se muestra como varios QR distintos, uno por acceso.
 */
export function DetalleOrdenPage() {
  const { id } = useParams();
  const location = useLocation();
  const recienConfirmada = (location.state as { recienConfirmada?: boolean } | null)?.recienConfirmada === true;
  const idOrden = Number(id);
  const idValido = Number.isInteger(idOrden) && idOrden > 0;
  const [orden, setOrden] = useState<OrdenDTO | null>(null);
  const [tickets, setTickets] = useState<TicketDTO[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<unknown>(null);
  const eventos = useEventos(orden?.items.map((item) => item.idEvento) ?? []);

  useEffect(() => {
    if (!idValido) return;
    let vigente = true;

    // 404 si la orden no existe o no es de quien pregunta: el backend no distingue los dos casos
    // a propósito, para no confirmar que una orden ajena existe.
    Promise.all([obtenerOrden(idOrden), obtenerTicketsDeOrden(idOrden)])
      .then(([ordenObtenida, ticketsObtenidos]) => {
        if (vigente) {
          setOrden(ordenObtenida);
          setTickets(ticketsObtenidos);
        }
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
  }, [idOrden, idValido]);

  if (!idValido) {
    return (
      <section className="stack-grande">
        <Link className="volver" to="/mis-ordenes">← Mis órdenes</Link>
        <p className="mensaje-error" role="alert">El identificador de la orden no es válido.</p>
      </section>
    );
  }

  if (cargando) return <EstadoCarga mensaje="Cargando la orden..." />;

  if (error || !orden) {
    return (
      <section className="stack-grande">
        <Link className="volver" to="/mis-ordenes">← Mis órdenes</Link>
        <EstadoError error={error} />
      </section>
    );
  }

  const cantidadTotal = orden.items.reduce((suma, item) => suma + item.cantidad, 0);

  return (
    <section className="stack-grande">
      {!recienConfirmada ? <Link className="volver" to="/mis-ordenes">← Mis órdenes</Link> : null}

      <div className="confirmacion">
        {recienConfirmada ? (
          <div className="icono-ok">
            <img className="icono" src={iconoCheckOk} width={48} height={48} alt="" />
          </div>
        ) : null}
        <h2>{recienConfirmada ? '¡Compra confirmada!' : `Orden #${orden.id}`}</h2>
        <p className="texto-secundario">
          {pluralEntradas(cantidadTotal)} a nombre de {orden.comprador.email}
        </p>
        <p className="texto-terciario">
          Orden #{orden.id} · {formatearFechaOrden(orden.creadaEn)} · Total cobrado {formatearPrecio(orden.total)}
        </p>

        <div className="grilla-entradas">
          {tickets.map((ticket) => {
            const evento = eventos.get(ticket.idEvento);
            const item = orden.items.find((linea) => linea.idTipoEntrada === ticket.idTipoEntrada);
            return (
              <article className="tarjeta-entrada" key={ticket.id}>
                <div className="banda-evento">
                  <h3>{nombreDeEvento(eventos, ticket.idEvento)}</h3>
                  {evento ? (
                    <p>{formatearFechaEvento(evento.fechaHora)} · {evento.lugar}</p>
                  ) : null}
                </div>
                <span className="tarjeta-entrada-tipo">{ticket.nombreTipoEntrada}</span>
                <img
                  className="ticket-qr"
                  src={`data:image/png;base64,${ticket.qrBase64}`}
                  width={220}
                  height={220}
                  alt={`QR de la entrada ${ticket.codigo}`}
                />
                <span className="tarjeta-entrada-precio">
                  {item ? formatearPrecio(item.precioUnitario) : 'Entrada emitida'}
                </span>
                <span className="ticket-codigo">Código {ticket.codigo}</span>
                <a
                  className="boton boton-secundario"
                  href={`data:image/png;base64,${ticket.qrBase64}`}
                  download={`passly-${ticket.codigo}.png`}
                >
                  Descargar QR
                </a>
              </article>
            );
          })}
        </div>

        {tickets.length === 0 ? (
          <p className="texto-terciario">Esta orden no tiene tickets emitidos.</p>
        ) : null}

        <div className="acciones">
          <Link className="boton boton-primario boton-sombra" to="/">Seguir comprando</Link>
          <Link className="boton boton-secundario" to="/mis-ordenes">Ir a mis órdenes</Link>
        </div>
      </div>
    </section>
  );
}
