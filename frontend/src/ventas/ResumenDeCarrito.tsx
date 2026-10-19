import type { CarritoDTO } from './types';
import { useEventos, nombreDeEvento } from './useEventos';
import { formatearFechaEvento, formatearPrecio } from './formato';
export function ResumenDeCarrito({ carrito }: { carrito: CarritoDTO }) {
  const ids = [...new Set(carrito.items.map((i) => i.idEvento))];
  const eventos = useEventos(ids);
  return (
    <aside className="resumen-compra" aria-label="Resumen de la compra">
      <h3>Resumen de tu compra</h3>
      {ids.map((id) => {
        const e = eventos.get(id);
        return (
          <div className="resumen-evento" key={id}>
            <h3>{nombreDeEvento(eventos, id)}</h3>
            {e ? (
              <p>
                {formatearFechaEvento(e.fechaHora)} · {e.lugar}
              </p>
            ) : (
              <p className="texto-secundario">
                Consultá la página del evento para confirmar fecha y lugar.
              </p>
            )}
            {carrito.items
              .filter((i) => i.idEvento === id)
              .map((i) => (
                <div className="resumen-linea" key={i.idTipoEntrada}>
                  <span>
                    {i.cantidad} × {i.nombreTipoEntrada}
                  </span>
                  <span>{formatearPrecio(i.precioUnitario * i.cantidad)}</span>
                </div>
              ))}
          </div>
        );
      })}
      <div className="resumen-total">
        <span>Total</span>
        <strong>{formatearPrecio(carrito.total)}</strong>
      </div>
    </aside>
  );
}
