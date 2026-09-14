import { useEffect, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { EstadoCarga } from '../layout/EstadoCarga';
import { EstadoError } from '../layout/EstadoError';
import { obtenerOrden } from './api';
import { formatearFechaEvento, formatearFechaOrden, formatearPrecio, pluralEntradas } from './formato';
import iconoCheckOk from './iconos/check-ok.svg';
import type { OrdenDTO } from './types';
import { nombreDeEvento, useEventos } from './useEventos';

/**
 * Compra confirmada / detalle de una orden (Figma Web 04).
 *
 * El diseño muestra una entrada con QR por persona y un botón de descarga: eso depende de
 * ServicioDeTickets, que todavía no existe. En su lugar va una tarjeta por línea de la orden con
 * lo que el backend sí devuelve, en vez de un QR de mentira.
 */
export function DetalleOrdenPage() {
  const { id } = useParams();
  const location = useLocation();
  const recienConfirmada = (location.state as { recienConfirmada?: boolean } | null)?.recienConfirmada === true;
  const idOrden = Number(id);
  const idValido = Number.isInteger(idOrden) && idOrden > 0;
  const [orden, setOrden] = useState<OrdenDTO | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<unknown>(null);
  const eventos = useEventos(orden?.items.map((item) => item.idEvento) ?? []);

  useEffect(() => {
    if (!idValido) return;
    let vigente = true;

    // 404 si la orden no existe o no es de quien pregunta: el backend no distingue los dos casos
    // a propósito, para no confirmar que una orden ajena existe.
    obtenerOrden(idOrden)
      .then((resultado) => {
        if (vigente) setOrden(resultado);
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
          {orden.items.map((item) => {
            const evento = eventos.get(item.idEvento);
            return (
              <article className="tarjeta-entrada" key={item.idTipoEntrada}>
                <div className="banda-evento">
                  <h3>{nombreDeEvento(eventos, item.idEvento)}</h3>
                  {evento ? (
                    <p>{formatearFechaEvento(evento.fechaHora)} · {evento.lugar}</p>
                  ) : null}
                </div>
                <span className="tarjeta-entrada-tipo">
                  {item.nombreTipoEntrada} · {pluralEntradas(item.cantidad)}
                </span>
                <span className="tarjeta-entrada-precio">{formatearPrecio(item.precioUnitario * item.cantidad)}</span>
                <span className="texto-terciario">{formatearPrecio(item.precioUnitario)} c/u</span>
              </article>
            );
          })}
        </div>

        <p className="texto-terciario">Los QR de acceso se emiten con ServicioDeTickets (próxima entrega).</p>

        <div className="acciones">
          <Link className="boton boton-primario boton-sombra" to="/">Seguir comprando</Link>
          <Link className="boton boton-secundario" to="/mis-ordenes">Ir a mis órdenes</Link>
        </div>
      </div>
    </section>
  );
}
