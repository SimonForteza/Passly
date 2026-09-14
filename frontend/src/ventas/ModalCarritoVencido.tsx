import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { EventoDTO } from '../eventos/types';
import { vaciarCarrito } from './api';
import iconoTimer from './iconos/timer.svg';
import type { CarritoDTO } from './types';
import { nombreDeEvento } from './useEventos';

/**
 * Carrito vencido (Figma Web 12). El copy se aparta del diseño a propósito: el carrito no reserva
 * cupo (trade-off declarado en CLAUDE.md §4.7), así que decir "liberamos tus entradas" sería
 * falso. Lo que pasa de verdad es que la selección ya no se puede confirmar.
 *
 * Las dos salidas abandonan el carrito (DELETE invalida la sesión): sin eso, el próximo carrito
 * de esta sesión heredaría el reloj vencido.
 */
export function ModalCarritoVencido({
  carrito,
  eventos,
}: {
  carrito: CarritoDTO;
  eventos: Map<number, EventoDTO | null>;
}) {
  const navigate = useNavigate();
  const primario = useRef<HTMLButtonElement>(null);
  const [saliendo, setSaliendo] = useState(false);

  useEffect(() => {
    primario.current?.focus();
  }, []);

  async function salirA(destino: string) {
    setSaliendo(true);
    await vaciarCarrito().catch(() => undefined);
    navigate(destino, { replace: true });
  }

  const idEventoParaVolver = carrito.items[0]?.idEvento;

  return (
    <div className="modal-fondo">
      <div className="modal-reserva" role="alertdialog" aria-modal="true" aria-labelledby="titulo-carrito-vencido">
        <div className="modal-icono">
          <img className="icono" src={iconoTimer} width={40} height={40} alt="" />
        </div>
        <h2 id="titulo-carrito-vencido">Tu carrito venció</h2>
        <p>
          Pasaron los 5 minutos del carrito y la selección ya no se puede confirmar. No se realizó
          ningún cobro.
        </p>
        {carrito.items.map((item) => (
          <div className="resumen-descartado" key={item.idTipoEntrada}>
            <span>
              {item.cantidad} × {item.nombreTipoEntrada} · {nombreDeEvento(eventos, item.idEvento)}
            </span>
            <strong>{item.cantidad === 1 ? 'descartada' : 'descartadas'}</strong>
          </div>
        ))}
        <div className="modal-acciones">
          <button
            ref={primario}
            className="boton boton-primario boton-sombra"
            type="button"
            disabled={saliendo}
            onClick={() => salirA(idEventoParaVolver ? `/eventos/${idEventoParaVolver}` : '/')}
          >
            Volver a elegir entradas
          </button>
          <button className="boton boton-secundario" type="button" disabled={saliendo} onClick={() => salirA('/')}>
            Ir a la cartelera
          </button>
        </div>
      </div>
    </div>
  );
}
