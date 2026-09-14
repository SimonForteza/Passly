import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { EstadoCarga } from '../layout/EstadoCarga';
import { EstadoError } from '../layout/EstadoError';
import { useAuth } from '../lib/auth/AuthContext';
import { obtenerEvento } from './api';
import type { EstadoEvento, EventoDTO, TipoEntradaDTO } from './types';
import { useDisponibilidad } from './useDisponibilidad';

const FORMATO_FECHA = new Intl.DateTimeFormat('es-AR', {
  weekday: 'long',
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
});

function claseBadgeEstado(estado: EstadoEvento): string {
  if (estado === 'PUBLICADO') return 'badge badge-publicado';
  if (estado === 'BORRADOR') return 'badge badge-borrador';
  return 'badge';
}

export function DetalleEventoPage() {
  const { id } = useParams();
  const { estaAutenticado } = useAuth();
  const idEvento = Number(id);
  const idValido = Number.isInteger(idEvento) && idEvento > 0;
  const [evento, setEvento] = useState<EventoDTO | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    let vigente = true;

    if (!idValido) return;

    obtenerEvento(idEvento)
      .then((resultado) => {
        if (vigente) setEvento(resultado);
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
  }, [idEvento, idValido]);

  if (!idValido) {
    return (
      <section className="stack-grande">
        <Link className="volver" to="/">← Cartelera</Link>
        <p className="mensaje-error" role="alert">El identificador del evento no es válido.</p>
      </section>
    );
  }

  if (cargando) return <EstadoCarga mensaje="Cargando el evento..." />;

  return (
    <section className="stack-grande">
      <Link className="volver" to="/">← Cartelera</Link>

      {error ? <EstadoError error={error} /> : null}

      {evento ? (
        <>
          <div className="encabezado-pagina">
            <div>
              <p className="eyebrow">{evento.organizador.nombreComercial}</p>
              <h2>{evento.nombre}</h2>
              <p className="texto-secundario">
                {FORMATO_FECHA.format(new Date(evento.fechaHora))} · {evento.lugar}
              </p>
            </div>
            <span className={claseBadgeEstado(evento.estado)}>{evento.estado}</span>
          </div>

          {evento.descripcion ? <p>{evento.descripcion}</p> : null}

          <div className="panel">
            <div className="titulo-seccion">
              <div>
                <p className="eyebrow">Entradas</p>
                <h3>Tipos de entrada</h3>
              </div>
            </div>

            <div className="tabla-responsive">
              <table>
                <thead>
                  <tr>
                    <th>Nombre</th>
                    <th>Precio</th>
                    <th>Disponibles</th>
                    {estaAutenticado ? <th /> : null}
                  </tr>
                </thead>
                <tbody>
                  {evento.tiposEntrada.map((tipo) => (
                    <FilaTipoEntrada key={tipo.id} tipo={tipo} mostrarActualizar={estaAutenticado} />
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      ) : null}
    </section>
  );
}

function FilaTipoEntrada({
  tipo,
  mostrarActualizar,
}: {
  tipo: TipoEntradaDTO;
  mostrarActualizar: boolean;
}) {
  // El hook es genérico (lo va a reusar el carrito de Ventas); acá solo se activa para
  // usuarios autenticados, porque el endpoint de disponibilidad exige credenciales y un
  // visitante anónimo ya tiene el cupo inicial embebido en el EventoDTO.
  const { disponibilidad, cargando, recargar } = useDisponibilidad(mostrarActualizar ? tipo.id : null);
  const cupo = disponibilidad?.cupoDisponible ?? tipo.cupoDisponible;

  return (
    <tr>
      <td>{tipo.nombre}</td>
      <td>${tipo.precio.toLocaleString('es-AR')}</td>
      <td>{cupo > 0 ? cupo : 'Agotado'}</td>
      {mostrarActualizar ? (
        <td>
          <button className="boton boton-secundario" type="button" onClick={recargar} disabled={cargando}>
            {cargando ? 'Actualizando...' : 'Actualizar'}
          </button>
        </td>
      ) : null}
    </tr>
  );
}
