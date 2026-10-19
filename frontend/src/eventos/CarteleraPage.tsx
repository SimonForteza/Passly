import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { EstadoCarga } from '../layout/EstadoCarga';
import { EstadoError } from '../layout/EstadoError';
import { listarProductoras } from '../productoras/api';
import type { ProductoraDTO } from '../productoras/types';
import { listarEventos } from './api';
import { TarjetaEvento } from './TarjetaEvento';
import type { EventoDTO } from './types';

// Minúsculas y sin tildes, para que "recital" encuentre "Recital" y "musica" encuentre "Música".
function normalizar(texto: string): string {
  return texto.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
}

export function CarteleraPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  // El filtro por productora vive en la URL (/?productora=3): es compartible y sobrevive a un F5.
  // La búsqueda por texto, en cambio, es estado local y filtra en el cliente sobre lo ya cargado.
  const filtroProductora = searchParams.get('productora') ?? '';
  const [busqueda, setBusqueda] = useState('');
  const [eventos, setEventos] = useState<EventoDTO[]>([]);
  const [productoras, setProductoras] = useState<ProductoraDTO[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    let vigente = true;
    setCargando(true);
    setError(null);

    const idProductora = filtroProductora
      ? Number(filtroProductora)
      : undefined;

    Promise.all([listarEventos(idProductora), listarProductoras()])
      .then(([listaEventos, listaProductoras]) => {
        if (!vigente) return;
        setEventos(listaEventos);
        setProductoras(listaProductoras);
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
  }, [filtroProductora]);

  function cambiarFiltro(idProductora: string) {
    if (idProductora) {
      setSearchParams({ productora: idProductora });
    } else {
      setSearchParams({});
    }
  }

  const termino = normalizar(busqueda.trim());
  const visibles = termino
    ? eventos.filter((evento) =>
        normalizar(`${evento.nombre} ${evento.lugar}`).includes(termino),
      )
    : eventos;

  return (
    <section className="stack-grande">
      <div className="encabezado-pagina">
        <div>
          <p className="eyebrow">Cartelera</p>
          <h2>Próximos eventos</h2>
          <p className="texto-secundario">
            Encontrá tu próximo evento y llevá tus entradas con QR.
          </p>
        </div>
      </div>

      <div className="filtros-cartelera">
        <label>
          Buscar
          <input
            type="search"
            value={busqueda}
            onChange={(evento) => setBusqueda(evento.target.value)}
            placeholder="Nombre del evento o lugar"
          />
        </label>
        <label>
          Productora
          <select
            value={filtroProductora}
            onChange={(evento) => cambiarFiltro(evento.target.value)}
          >
            <option value="">Todas las productoras</option>
            {productoras.map((productora) => (
              <option key={productora.id} value={productora.id}>
                {productora.nombreComercial}
              </option>
            ))}
          </select>
        </label>
      </div>

      {error ? <EstadoError error={error} /> : null}

      {cargando ? (
        <EstadoCarga mensaje="Cargando la cartelera..." />
      ) : eventos.length === 0 ? (
        <div className="panel estado-vacio">
          <h3>No hay eventos publicados</h3>
          <p className="texto-secundario">
            {filtroProductora
              ? 'Esta productora no tiene eventos publicados por ahora.'
              : 'Volvé a mirar más tarde.'}
          </p>
        </div>
      ) : visibles.length === 0 ? (
        <div className="panel estado-vacio">
          <h3>Ningún evento coincide con “{busqueda.trim()}”</h3>
          <p className="texto-secundario">Probá con otro nombre o lugar.</p>
        </div>
      ) : (
        <div className="grilla-tarjetas">
          {visibles.map((evento) => (
            <TarjetaEvento key={evento.id} evento={evento} />
          ))}
        </div>
      )}
    </section>
  );
}
