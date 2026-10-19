import { useEffect, useRef, useState } from 'react';
import { EstadoError } from '../layout/EstadoError';
import { formatearFechaEvento, formatearPrecio } from '../ventas/formato';
import { publicarEvento } from './api';
import type { EventoDTO } from './types';
export function PublicarEvento({
  evento,
  onPublicado,
}: {
  evento: EventoDTO;
  onPublicado: (evento: EventoDTO) => void;
}) {
  const dialogo = useRef<HTMLDialogElement>(null);
  const [abierto, setAbierto] = useState(false);
  const [ocupado, setOcupado] = useState(false);
  const [error, setError] = useState<unknown>(null);
  useEffect(() => {
    if (abierto) dialogo.current?.showModal();
    else dialogo.current?.close();
  }, [abierto]);
  async function publicar() {
    setOcupado(true);
    setError(null);
    try {
      const actualizado = await publicarEvento(evento.id);
      onPublicado(actualizado);
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
        className="boton boton-primario"
        onClick={() => {
          setError(null);
          setAbierto(true);
        }}
      >
        Revisar y publicar
      </button>
      <dialog
        ref={dialogo}
        className="dialogo-publicacion"
        aria-labelledby={'publicar-' + evento.id}
        onCancel={(e) => {
          if (ocupado) e.preventDefault();
          else setAbierto(false);
        }}
        onClose={() => setAbierto(false)}
      >
        <div className="stack-grande">
          <div>
            <p className="eyebrow">Antes de publicar</p>
            <h2 id={'publicar-' + evento.id}>Revisá tu evento</h2>
          </div>
          <div>
            <h3>{evento.nombre}</h3>
            <p>
              {formatearFechaEvento(evento.fechaHora)} · {evento.lugar}
            </p>
          </div>
          <ul className="lista-resumen">
            {evento.tiposEntrada.map((t) => (
              <li key={t.id}>
                <strong>{t.nombre}</strong>
                <span>
                  {formatearPrecio(t.precio)} · {t.cupoTotal} entradas
                </span>
              </li>
            ))}
          </ul>
          <p>
            Al publicar, el evento aparecerá en la cartelera y sus entradas
            quedarán a la venta.
          </p>
          <p className="aviso-informativo">
            Después no podrás cambiar el nombre, la fecha, el lugar ni las
            categorías existentes. Sí podrás sumar cupo y agregar categorías.
          </p>
          {error ? <EstadoError error={error} /> : null}
          <div className="acciones">
            <button
              className="boton boton-primario"
              disabled={ocupado}
              onClick={publicar}
            >
              {ocupado ? 'Publicando…' : 'Publicar evento'}
            </button>
            <button
              className="boton boton-secundario"
              disabled={ocupado}
              onClick={() => setAbierto(false)}
            >
              Conservar borrador
            </button>
          </div>
        </div>
      </dialog>
    </>
  );
}
