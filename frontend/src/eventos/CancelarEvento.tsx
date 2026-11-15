import { useEffect, useRef, useState } from 'react';
import { EstadoError } from '../layout/EstadoError';
import { formatearFechaEvento } from '../ventas/formato';
import { cancelarEvento } from './api';
import type { EventoDTO } from './types';

/**
 * Botón "Cancelar evento" + diálogo de confirmación (PAS-35). Mismo patrón <dialog> que
 * PublicarEvento. La regla de qué estados se pueden cancelar vive en el backend (patrón State):
 * si el evento ya estaba cancelado, el 409 llega como ProblemDetail y se muestra en EstadoError.
 */
export function CancelarEvento({
  evento,
  onCancelado,
}: {
  evento: EventoDTO;
  onCancelado: (evento: EventoDTO) => void;
}) {
  const dialogo = useRef<HTMLDialogElement>(null);
  const [abierto, setAbierto] = useState(false);
  const [ocupado, setOcupado] = useState(false);
  const [error, setError] = useState<unknown>(null);
  useEffect(() => {
    if (abierto) dialogo.current?.showModal();
    else dialogo.current?.close();
  }, [abierto]);
  async function cancelar() {
    setOcupado(true);
    setError(null);
    try {
      const actualizado = await cancelarEvento(evento.id);
      onCancelado(actualizado);
      setAbierto(false);
    } catch (e) {
      setError(e);
    } finally {
      setOcupado(false);
    }
  }
  return (
    <>
      <button
        className="boton boton-peligro"
        onClick={() => {
          setError(null);
          setAbierto(true);
        }}
      >
        Cancelar evento
      </button>
      <dialog
        ref={dialogo}
        className="dialogo-publicacion"
        aria-labelledby={'cancelar-' + evento.id}
        onCancel={(e) => {
          if (ocupado) e.preventDefault();
          else setAbierto(false);
        }}
        onClose={() => setAbierto(false)}
      >
        <div className="stack-grande">
          <div>
            <p className="eyebrow">Cancelar evento</p>
            <h2 id={'cancelar-' + evento.id}>¿Cancelar «{evento.nombre}»?</h2>
            <p>
              {formatearFechaEvento(evento.fechaHora)} · {evento.lugar}
            </p>
          </div>
          <p>
            El evento dejará de aparecer en la cartelera y no se podrán vender
            más entradas. Quien abra su enlace verá que fue cancelado.
          </p>
          <p className="aviso-informativo">
            Esta acción no se puede deshacer. Por ahora Passly no reintegra las
            entradas ya vendidas ni avisa a los compradores.
          </p>
          {error ? <EstadoError error={error} /> : null}
          <div className="acciones">
            <button
              className="boton boton-primario"
              disabled={ocupado}
              onClick={cancelar}
            >
              {ocupado ? 'Cancelando…' : 'Sí, cancelar evento'}
            </button>
            <button
              className="boton boton-secundario"
              disabled={ocupado}
              onClick={() => setAbierto(false)}
            >
              Volver
            </button>
          </div>
        </div>
      </dialog>
    </>
  );
}
