import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { listarEventos } from '../eventos/api';
import { TarjetaEvento } from '../eventos/TarjetaEvento';
import type { EventoDTO } from '../eventos/types';
import { EstadoCarga } from '../layout/EstadoCarga';
import { EstadoError } from '../layout/EstadoError';
import { ApiError } from '../lib/http/ApiError';
import { obtenerProductora } from './api';
import type { ProductoraDTO } from './types';

// Página pública de una productora (PAS-26): lo que ve quien llega desde una productora, sin
// sesión. Solo eventos publicados — es la misma cartelera filtrada (GET /api/eventos?productora=),
// no el backoffice, que vive en /productoras/:id/eventos y sí incluye borradores.
export function PaginaProductoraPage() {
  const { id } = useParams();
  const idProductora = Number(id);
  const idValido = Number.isInteger(idProductora) && idProductora > 0;
  const [productora, setProductora] = useState<ProductoraDTO | null>(null);
  const [eventos, setEventos] = useState<EventoDTO[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    let vigente = true;

    if (!idValido) return;

    Promise.all([obtenerProductora(idProductora), listarEventos(idProductora)])
      .then(([detalle, lista]) => {
        if (!vigente) return;
        setProductora(detalle);
        setEventos(lista);
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
  }, [idProductora, idValido]);

  if (!idValido || (error instanceof ApiError && error.status === 404)) {
    return (
      <section className="stack-grande">
        <Link className="volver" to="/">← Cartelera</Link>
        <div className="panel estado-vacio">
          <h3>Esta productora no existe</h3>
          <p className="texto-secundario">Revisá el link o buscá sus eventos en la cartelera.</p>
        </div>
      </section>
    );
  }

  if (cargando) return <EstadoCarga mensaje="Cargando la productora..." />;

  return (
    <section className="stack-grande">
      <Link className="volver" to="/">← Cartelera</Link>

      {error ? <EstadoError error={error} /> : null}

      {productora ? (
        <>
          <div className="encabezado-pagina">
            <div>
              <p className="eyebrow">Productora</p>
              <h2>{productora.nombreComercial}</h2>
              {productora.descripcion ? (
                <p className="texto-secundario">{productora.descripcion}</p>
              ) : null}
            </div>
          </div>

          {eventos.length === 0 ? (
            <div className="panel estado-vacio">
              <h3>Esta productora todavía no tiene eventos publicados</h3>
              <p className="texto-secundario">Volvé a mirar más tarde.</p>
            </div>
          ) : (
            <div className="grilla-tarjetas">
              {eventos.map((evento) => (
                <TarjetaEvento key={evento.id} evento={evento} mostrarOrganizador={false} />
              ))}
            </div>
          )}
        </>
      ) : null}
    </section>
  );
}
