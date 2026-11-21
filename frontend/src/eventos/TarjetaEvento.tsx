import { Link } from 'react-router-dom';
import { urlDeApi } from '../lib/http/httpClient';
import type { EventoDTO } from './types';

const DIA = new Intl.DateTimeFormat('es-AR', { weekday: 'short' });
const MES = new Intl.DateTimeFormat('es-AR', { month: 'short' });
// h23: es-AR usa reloj de 12 h por defecto ("12:11 a. m."); el diseño muestra 24 h.
const HORA = new Intl.DateTimeFormat('es-AR', {
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
});

/**
 * "SÁB 28 NOV · 23:00", como en el diseño (Figma Web 25). Se arma por partes porque el formato
 * conjunto de día y mes de es-AR sale como "28-nov".
 */
function fechaCorta(fechaHora: string): string {
  const fecha = new Date(fechaHora);
  const limpiar = (s: string) => s.replace('.', '').toUpperCase();
  const dia = String(fecha.getDate()).padStart(2, '0');
  return `${limpiar(DIA.format(fecha))} ${dia} ${limpiar(MES.format(fecha))} · ${HORA.format(fecha)}`;
}

function estaAgotado(evento: EventoDTO): boolean {
  return (
    evento.tiposEntrada.length > 0 &&
    evento.tiposEntrada.every((tipo) => tipo.cupoDisponible === 0)
  );
}

/**
 * Un evento en la grilla, al estilo FlashPass (PAS-47): la portada vertical 4:5 y debajo fecha,
 * nombre y lugar. Toda la tarjeta es el link al detalle; no hay botón ni precio. La usan la
 * cartelera y la página pública de cada productora (PAS-26); en esa última el nombre de la
 * productora ya está en el encabezado, así que se omite con `mostrarOrganizador={false}`.
 */
export function TarjetaEvento({
  evento,
  mostrarOrganizador = true,
}: {
  evento: EventoDTO;
  mostrarOrganizador?: boolean;
}) {
  const agotado = estaAgotado(evento);
  const cancelado = evento.estado === 'CANCELADO';
  const etiqueta = cancelado ? 'Cancelado' : agotado ? 'Agotado' : null;

  return (
    <Link
      className={'tarjeta-portada' + (etiqueta ? ' tarjeta-portada-apagada' : '')}
      to={`/eventos/${evento.id}`}
    >
      <div className="tarjeta-portada-imagen">
        {evento.portadaUrl ? (
          <img src={urlDeApi(evento.portadaUrl)} alt="" loading="lazy" decoding="async" />
        ) : (
          // Respaldo sin portada: el nombre sobre el degradado de la marca.
          <span className="portada-respaldo" aria-hidden="true">
            {evento.nombre}
          </span>
        )}
        {etiqueta ? <span className="tarjeta-portada-etiqueta">{etiqueta}</span> : null}
      </div>
      <div className="tarjeta-portada-info">
        <span className="tarjeta-portada-fecha">{fechaCorta(evento.fechaHora)}</span>
        <h3>{evento.nombre}</h3>
        <span className="tarjeta-portada-lugar">
          {evento.lugar}
          {mostrarOrganizador ? ` · ${evento.organizador.nombreComercial}` : ''}
        </span>
      </div>
    </Link>
  );
}
