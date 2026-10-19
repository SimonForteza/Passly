import { formatearFechaEvento, formatearPrecio } from './formato';
import { nombreDeEvento, useEventos } from './useEventos';
import type { OrdenDTO, TicketDTO } from './types';
export function EntradasDeOrden({
  orden,
  tickets,
}: {
  orden: OrdenDTO;
  tickets: TicketDTO[];
}) {
  const eventos = useEventos(orden.items.map((i) => i.idEvento));
  return (
    <>
      <p className="aviso-informativo">
        Mostrá un QR por persona en el ingreso. Cada entrada se puede usar una
        sola vez. Guardá tus QR para tenerlos a mano.
      </p>
      <div className="grilla-entradas">
        {tickets.map((t) => {
          const evento = eventos.get(t.idEvento);
          const item = orden.items.find(
            (i) => i.idTipoEntrada === t.idTipoEntrada,
          );
          return (
            <article className="tarjeta-entrada" key={t.id}>
              <div className="banda-evento">
                <h3>{nombreDeEvento(eventos, t.idEvento)}</h3>
                {evento ? (
                  <p>
                    {formatearFechaEvento(evento.fechaHora)} · {evento.lugar}
                  </p>
                ) : (
                  <p className="texto-secundario">
                    Si no se carga la fecha o el lugar, consultá la página del
                    evento.
                  </p>
                )}
              </div>
              <strong>{t.nombreTipoEntrada}</strong>
              <span className="badge">
                {t.estado === 'USADO'
                  ? 'Entrada utilizada'
                  : 'Lista para ingresar'}
              </span>
              <img
                className="ticket-qr"
                src={'data:image/png;base64,' + t.qrBase64}
                width={220}
                height={220}
                alt={
                  'QR de tu entrada para ' + nombreDeEvento(eventos, t.idEvento)
                }
              />
              {item ? (
                <span>{formatearPrecio(item.precioUnitario)}</span>
              ) : null}
              <span className="ticket-codigo">Código {t.codigo}</span>
              <details className="ticket-contenido">
                <summary>Alternativa para ingreso manual</summary>
                <p>
                  Mostrá este contenido al personal de acceso si no puede leer
                  el QR.
                </p>
                <code>{t.contenidoQr}</code>
              </details>
              <a
                className="boton boton-secundario"
                href={'data:image/png;base64,' + t.qrBase64}
                download={'passly-' + t.codigo + '.png'}
              >
                Descargar QR
              </a>
            </article>
          );
        })}
      </div>
      {!tickets.length ? (
        <p>No encontramos entradas emitidas para esta orden.</p>
      ) : null}
    </>
  );
}
