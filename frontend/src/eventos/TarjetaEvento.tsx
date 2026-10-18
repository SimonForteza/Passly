import { Link } from 'react-router-dom';
import type { EventoDTO } from './types';

const FORMATO_FECHA = new Intl.DateTimeFormat('es-AR', {
  weekday: 'short',
  day: '2-digit',
  month: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
});

function precioDesde(evento: EventoDTO): number | null {
  if (evento.tiposEntrada.length === 0) return null;
  return Math.min(...evento.tiposEntrada.map((tipo) => tipo.precio));
}

// La tarjeta de un evento publicado en una grilla: la usan la cartelera y la página pública de
// cada productora (PAS-26). En la página de la productora el nombre ya está en el encabezado, así
// que ahí se omite con `mostrarOrganizador={false}`.
export function TarjetaEvento({
  evento,
  mostrarOrganizador = true,
}: {
  evento: EventoDTO;
  mostrarOrganizador?: boolean;
}) {
  const desde = precioDesde(evento);
  return (
    <article className="panel tarjeta-evento">
      <div>
        {mostrarOrganizador ? <p className="eyebrow">{evento.organizador.nombreComercial}</p> : null}
        <h3>{evento.nombre}</h3>
        <p className="texto-secundario">
          {FORMATO_FECHA.format(new Date(evento.fechaHora))} · {evento.lugar}
        </p>
      </div>
      <div className="tarjeta-evento-pie">
        <span className="texto-secundario">
          {desde !== null ? `Desde $${desde.toLocaleString('es-AR')}` : 'Sin entradas cargadas'}
        </span>
        <Link className="boton boton-primario" to={`/eventos/${evento.id}`}>
          Ver entradas
        </Link>
      </div>
    </article>
  );
}
