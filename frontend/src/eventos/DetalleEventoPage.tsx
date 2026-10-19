import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { EstadoCarga } from '../layout/EstadoCarga';
import { EstadoError } from '../layout/EstadoError';
import { agregarInvitado } from '../ventas/invitadosApi';
import { useAuth } from '../lib/auth/AuthContext';
import { ApiError } from '../lib/http/ApiError';
import { agregarAlCarrito } from '../ventas/agregarAlCarrito';
import { formatearPrecio } from '../ventas/formato';
import { useOrganizaciones } from '../productoras/OrganizacionContext';
import iconoReloj from '../ventas/iconos/reloj-acento.svg';
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

// El cupo se vuelve a comprobar al confirmar: el carrito no reserva entradas.
const MAXIMO_POR_TIPO = 20;
const CUPO_ESCASO = 20;

function claseBadgeEstado(estado: EstadoEvento): string {
  if (estado === 'PUBLICADO') return 'badge badge-publicado';
  if (estado === 'BORRADOR') return 'badge badge-borrador';
  return 'badge';
}

export function DetalleEventoPage() {
  const { id } = useParams();
  const { estaAutenticado } = useAuth();
  const { organizaciones } = useOrganizaciones();
  const navigate = useNavigate();
  const idEvento = Number(id);
  const idValido = Number.isInteger(idEvento) && idEvento > 0;
  const [evento, setEvento] = useState<EventoDTO | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<unknown>(null);
  const [cantidades, setCantidades] = useState<Record<number, number>>({});
  const [agregando, setAgregando] = useState(false);
  const [errorAlAgregar, setErrorAlAgregar] = useState<unknown>(null);

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
        <Link className="volver" to="/">
          ← Cartelera
        </Link>
        <p className="mensaje-error" role="alert">
          El identificador del evento no es válido.
        </p>
      </section>
    );
  }

  if (cargando) return <EstadoCarga mensaje="Cargando el evento..." />;

  const aLaVenta = evento?.estado === 'PUBLICADO';
  const gestion = organizaciones.find(
    (p) => p.id === evento?.organizador.id && p.puedeGestionar,
  );
  const seleccion = (evento?.tiposEntrada ?? [])
    .map((tipo) => ({ tipo, cantidad: cantidades[tipo.id] ?? 0 }))
    .filter(({ cantidad }) => cantidad > 0);
  const total = seleccion.reduce(
    (suma, { tipo, cantidad }) => suma + tipo.precio * cantidad,
    0,
  );

  function cambiarCantidad(idTipoEntrada: number, cantidad: number) {
    setCantidades((actuales) => ({ ...actuales, [idTipoEntrada]: cantidad }));
  }

  async function continuarAlPago() {
    setAgregando(true);
    setErrorAlAgregar(null);
    try {
      await (estaAutenticado ? agregarAlCarrito : agregarInvitado)(
        seleccion.map(({ tipo, cantidad }) => ({
          idTipoEntrada: tipo.id,
          cantidad,
        })),
      );
      navigate('/carrito');
    } catch (causa) {
      setErrorAlAgregar(causa);
      setAgregando(false);
    }
  }

  return (
    <section className="stack-grande">
      <Link className="volver" to="/">
        ← Cartelera
      </Link>

      {/* 404 es tanto "no existe" como "es un borrador que no podés ver" (PAS-25): el backend no
          distingue a propósito, y la pantalla tampoco. */}
      {error instanceof ApiError && error.status === 404 ? (
        <div className="panel estado-vacio">
          <h3>Este evento no existe o todavía no está publicado</h3>
          <p className="texto-secundario">
            Revisá el link o buscá el evento en la cartelera.
          </p>
        </div>
      ) : error ? (
        <EstadoError error={error} />
      ) : null}

      {evento ? (
        <>
          {gestion ? (
            <div className="aviso-informativo">
              Estás viendo la página para el público.{' '}
              {evento.estado === 'BORRADOR'
                ? 'Este borrador todavía no está a la venta. '
                : ''}
              <Link to={`/productoras/${gestion.id}/eventos/${evento.id}`}>
                Volver a gestionar el evento
              </Link>
            </div>
          ) : null}
          <div className="encabezado-pagina">
            <div>
              <p className="eyebrow">
                {/* Desde el evento se llega a todo lo que publicó su productora (PAS-26). */}
                <Link to={`/productoras/${evento.organizador.id}`}>
                  {evento.organizador.nombreComercial}
                </Link>
              </p>
              <h2>{evento.nombre}</h2>
              <p className="texto-secundario">
                {FORMATO_FECHA.format(new Date(evento.fechaHora))} ·{' '}
                {evento.lugar}
              </p>
            </div>
            <span className={claseBadgeEstado(evento.estado)}>
              {evento.estado === 'PUBLICADO'
                ? 'A la venta'
                : evento.estado === 'BORRADOR'
                  ? 'Borrador'
                  : 'Cancelado'}
            </span>
          </div>

          {evento.descripcion ? <p>{evento.descripcion}</p> : null}

          <div className="checkout">
            <div className="checkout-columna">
              <h3>{aLaVenta ? 'Elegí tus entradas' : 'Entradas del evento'}</h3>
              {evento.estado === 'CANCELADO' ? (
                <p className="texto-secundario">Este evento fue cancelado.</p>
              ) : !aLaVenta ? (
                <p className="texto-secundario">
                  Este evento todavía no está a la venta.
                </p>
              ) : null}
              {evento.tiposEntrada.length === 0 ? (
                <p className="texto-secundario">
                  Este evento no tiene tipos de entrada cargados.
                </p>
              ) : null}
              {evento.tiposEntrada.map((tipo) => (
                <FilaTipoEntrada
                  key={tipo.id}
                  tipo={tipo}
                  consultarCupo={estaAutenticado}
                  habilitada={aLaVenta}
                  cantidad={cantidades[tipo.id] ?? 0}
                  onCambio={(cantidad) => cambiarCantidad(tipo.id, cantidad)}
                />
              ))}
            </div>

            {aLaVenta ? (
              <aside
                className="resumen-compra"
                aria-label="Resumen de la compra"
              >
                <h3>Tu compra</h3>
                {seleccion.length === 0 ? (
                  <p className="texto-secundario" style={{ margin: 0 }}>
                    Sumá entradas con los botones + para verlas acá.
                  </p>
                ) : (
                  seleccion.map(({ tipo, cantidad }) => (
                    <div className="resumen-linea" key={tipo.id}>
                      <span>
                        {cantidad} × {tipo.nombre}
                      </span>
                      <span>{formatearPrecio(tipo.precio * cantidad)}</span>
                    </div>
                  ))
                )}
                <div className="resumen-divisor" />
                <div className="resumen-total">
                  <span>Total</span>
                  <span>{formatearPrecio(total)}</span>
                </div>
                <div className="aviso-hold">
                  <img
                    className="icono"
                    src={iconoReloj}
                    width={18}
                    height={18}
                    alt=""
                  />
                  <span>
                    La selección dura 5 minutos y no reserva cupo. Comprobamos
                    disponibilidad al confirmar.
                  </span>
                </div>
                {errorAlAgregar ? <EstadoError error={errorAlAgregar} /> : null}
                <button
                  className="boton boton-primario boton-ancho"
                  type="button"
                  onClick={continuarAlPago}
                  disabled={!aLaVenta || seleccion.length === 0 || agregando}
                >
                  {agregando
                    ? 'Agregando al carrito...'
                    : estaAutenticado
                      ? 'Continuar al pago'
                      : 'Continuar como invitado'}
                </button>
                <p className="aviso-pasarela">
                  En el checkout te vamos a pedir los datos de una tarjeta de
                  prueba. La pasarela no realiza cobros reales.
                </p>
              </aside>
            ) : null}
          </div>
        </>
      ) : null}
    </section>
  );
}

function FilaTipoEntrada({
  tipo,
  consultarCupo,
  habilitada,
  cantidad,
  onCambio,
}: {
  tipo: TipoEntradaDTO;
  consultarCupo: boolean;
  habilitada: boolean;
  cantidad: number;
  onCambio: (cantidad: number) => void;
}) {
  // El endpoint de disponibilidad exige credenciales: un visitante anónimo usa el cupo que ya
  // viene embebido en el EventoDTO.
  const { disponibilidad, cargando, recargar } = useDisponibilidad(
    consultarCupo ? tipo.id : null,
  );
  const cupo = disponibilidad?.cupoDisponible ?? tipo.cupoDisponible;
  const agotado = cupo <= 0;
  const clases =
    cantidad > 0 ? 'fila-entrada fila-entrada-seleccionada' : 'fila-entrada';

  return (
    <div className={clases}>
      <div className="fila-entrada-info">
        <p className="fila-entrada-nombre">{tipo.nombre}</p>
        {habilitada ? (
          <p
            className={
              !agotado && cupo <= CUPO_ESCASO ? 'cupo-escaso' : undefined
            }
          >
            {agotado
              ? 'Agotado'
              : cupo <= CUPO_ESCASO
                ? `¡Quedan ${cupo}!`
                : `Quedan ${cupo}`}
            {consultarCupo ? (
              <button
                className="boton-recargar"
                type="button"
                onClick={recargar}
                disabled={cargando}
                aria-label={`Actualizar el cupo de ${tipo.nombre}`}
                title="Actualizar cupo"
              >
                {cargando ? '…' : '↻'}
              </button>
            ) : null}
          </p>
        ) : (
          <p className="texto-secundario">
            {tipo.cupoTotal} entradas previstas
          </p>
        )}
        {cantidad > cupo && !agotado ? (
          <p className="aviso-sobrecupo">
            Pediste más de las que quedan: la compra puede rechazarse.
          </p>
        ) : null}
      </div>
      <div className="fila-entrada-selector">
        <span>{formatearPrecio(tipo.precio)}</span>
        {habilitada ? (
          <div className="stepper">
            <button
              type="button"
              onClick={() => onCambio(cantidad - 1)}
              disabled={!habilitada || cantidad === 0}
              aria-label={`Quitar una entrada ${tipo.nombre}`}
            >
              −
            </button>
            <output aria-live="polite">{cantidad}</output>
            <button
              type="button"
              onClick={() => onCambio(cantidad + 1)}
              disabled={
                !habilitada ||
                agotado ||
                cantidad >= Math.min(cupo, MAXIMO_POR_TIPO)
              }
              aria-label={`Sumar una entrada ${tipo.nombre}`}
            >
              +
            </button>
          </div>
        ) : null}
      </div>
    </div>
  );
}
