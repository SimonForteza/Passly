import { useEffect, useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { EstadoCarga } from '../layout/EstadoCarga';
import { EstadoError } from '../layout/EstadoError';
import { useAuth } from '../lib/auth/AuthContext';
import { agregarTipoEntrada, ampliarCupo, editarEvento, editarTipoEntrada, obtenerEvento } from './api';
import type { CrearTipoEntradaRequest, EventoDTO, TipoEntradaDTO } from './types';

const TIPO_ENTRADA_INICIAL: CrearTipoEntradaRequest = { nombre: '', precio: 0, cupoTotal: 0 };

// Parte un ISO ("2027-03-15T21:00:00-03:00") en los valores de <input type="date">/<input
// type="time"> en hora local, y arma el ISO de vuelta con el mismo criterio que CrearEventoPage.
function partirFechaHora(iso: string): { fecha: string; hora: string } {
  const fechaHora = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  const fecha = `${fechaHora.getFullYear()}-${pad(fechaHora.getMonth() + 1)}-${pad(fechaHora.getDate())}`;
  const hora = `${pad(fechaHora.getHours())}:${pad(fechaHora.getMinutes())}`;
  return { fecha, hora };
}

function FilaTipoEntrada({
  tipo,
  soloAmpliar,
  onGuardar,
  onAmpliar,
}: {
  tipo: TipoEntradaDTO;
  soloAmpliar: boolean;
  onGuardar: (cambios: { nombre: string; precio: number; cupoTotal: number }) => Promise<void>;
  onAmpliar: (cantidad: number) => Promise<void>;
}) {
  const vendidas = tipo.cupoTotal - tipo.cupoDisponible;
  const [nombre, setNombre] = useState(tipo.nombre);
  const [precio, setPrecio] = useState(tipo.precio);
  const [cupoTotal, setCupoTotal] = useState(tipo.cupoTotal);
  const [cantidadASumar, setCantidadASumar] = useState(0);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState<unknown>(null);

  async function guardar() {
    setGuardando(true);
    setError(null);
    try {
      await onGuardar({ nombre: nombre.trim(), precio, cupoTotal });
    } catch (causa: unknown) {
      setError(causa);
    } finally {
      setGuardando(false);
    }
  }

  async function sumar() {
    if (cantidadASumar <= 0) return;
    setGuardando(true);
    setError(null);
    try {
      await onAmpliar(cantidadASumar);
      setCantidadASumar(0);
    } catch (causa: unknown) {
      setError(causa);
    } finally {
      setGuardando(false);
    }
  }

  return (
    <div className="panel">
      <div className="fila-tipo-entrada">
        <label>
          Nombre
          <input
            maxLength={80}
            value={nombre}
            disabled={soloAmpliar}
            onChange={(e) => setNombre(e.target.value)}
          />
        </label>
        <label>
          Precio
          <input
            type="number"
            min={0}
            step="0.01"
            value={precio}
            disabled={soloAmpliar}
            onChange={(e) => setPrecio(Number(e.target.value))}
          />
        </label>
        <label>
          Cupo total
          <input
            type="number"
            min={vendidas}
            step={1}
            value={cupoTotal}
            disabled={soloAmpliar}
            onChange={(e) => setCupoTotal(Number(e.target.value))}
          />
        </label>
        {!soloAmpliar ? (
          <button className="boton boton-secundario" type="button" onClick={guardar} disabled={guardando}>
            {guardando ? 'Guardando...' : 'Guardar'}
          </button>
        ) : null}
      </div>

      <p className="ayuda-campo">
        Vendidas: {vendidas} · Disponibles: {tipo.cupoDisponible}
      </p>

      <div className="fila-tipo-entrada">
        <label>
          Sumar entradas
          <input
            type="number"
            min={1}
            step={1}
            value={cantidadASumar}
            onChange={(e) => setCantidadASumar(Number(e.target.value))}
          />
        </label>
        <button
          className="boton boton-secundario"
          type="button"
          onClick={sumar}
          disabled={guardando || cantidadASumar <= 0}
        >
          {guardando ? 'Sumando...' : 'Sumar cupo'}
        </button>
      </div>

      {error ? <EstadoError error={error} /> : null}
    </div>
  );
}

export function EditarEventoPage() {
  const { id, idEvento } = useParams();
  const { usuario } = useAuth();
  const idProductora = Number(id);
  const idEventoNumero = Number(idEvento);
  const idsValidos =
    Number.isInteger(idProductora) && idProductora > 0 && Number.isInteger(idEventoNumero) && idEventoNumero > 0;

  const [evento, setEvento] = useState<EventoDTO | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<unknown>(null);

  const [nombre, setNombre] = useState('');
  const [descripcion, setDescripcion] = useState('');
  const [fecha, setFecha] = useState('');
  const [hora, setHora] = useState('');
  const [lugar, setLugar] = useState('');
  const [guardandoDatos, setGuardandoDatos] = useState(false);
  const [errorDatos, setErrorDatos] = useState<unknown>(null);

  const [nuevoTipo, setNuevoTipo] = useState<CrearTipoEntradaRequest>({ ...TIPO_ENTRADA_INICIAL });
  const [agregando, setAgregando] = useState(false);
  const [errorAgregar, setErrorAgregar] = useState<unknown>(null);

  const puedeEditar = usuario?.rol === 'ORGANIZADOR';

  useEffect(() => {
    let vigente = true;
    if (!idsValidos) return;

    obtenerEvento(idEventoNumero)
      .then((detalle) => {
        if (!vigente) return;
        setEvento(detalle);
        const { fecha: f, hora: h } = partirFechaHora(detalle.fechaHora);
        setNombre(detalle.nombre);
        setDescripcion(detalle.descripcion ?? '');
        setFecha(f);
        setHora(h);
        setLugar(detalle.lugar);
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
  }, [idEventoNumero, idsValidos]);

  async function guardarDatos(evt: FormEvent<HTMLFormElement>) {
    evt.preventDefault();
    setGuardandoDatos(true);
    setErrorDatos(null);
    try {
      const fechaHora = new Date(`${fecha}T${hora}`).toISOString();
      const actualizado = await editarEvento(idEventoNumero, {
        nombre: nombre.trim(),
        descripcion: descripcion.trim(),
        fechaHora,
        lugar: lugar.trim(),
      });
      setEvento(actualizado);
    } catch (causa: unknown) {
      setErrorDatos(causa);
    } finally {
      setGuardandoDatos(false);
    }
  }

  async function agregarTipo(evt: FormEvent<HTMLFormElement>) {
    evt.preventDefault();
    setAgregando(true);
    setErrorAgregar(null);
    try {
      const actualizado = await agregarTipoEntrada(idEventoNumero, {
        nombre: nuevoTipo.nombre.trim(),
        precio: nuevoTipo.precio,
        cupoTotal: nuevoTipo.cupoTotal,
      });
      setEvento(actualizado);
      setNuevoTipo({ ...TIPO_ENTRADA_INICIAL });
    } catch (causa: unknown) {
      setErrorAgregar(causa);
    } finally {
      setAgregando(false);
    }
  }

  if (!idsValidos) {
    return (
      <section className="stack-grande">
        <Link className="volver" to="/productoras">← Mis productoras</Link>
        <p className="mensaje-error" role="alert">El identificador del evento no es válido.</p>
      </section>
    );
  }

  if (!puedeEditar) {
    return (
      <section className="stack-grande">
        <Link className="volver" to={`/productoras/${idProductora}/eventos`}>← Eventos de la productora</Link>
        <p className="mensaje-error" role="alert">Necesitás el rol ORGANIZADOR para editar un evento.</p>
      </section>
    );
  }

  if (cargando) return <EstadoCarga mensaje="Cargando el evento..." />;
  if (error) return <EstadoError error={error} />;
  if (!evento) return null;

  const enBorrador = evento.estado === 'BORRADOR';
  const cancelado = evento.estado === 'CANCELADO';

  return (
    <section className="stack-grande">
      <Link className="volver" to={`/productoras/${idProductora}/eventos`}>← Eventos de la productora</Link>

      <div>
        <p className="eyebrow">Editar evento</p>
        <h2>{evento.nombre}</h2>
        {enBorrador ? (
          <p className="texto-secundario">En borrador: se puede editar todo.</p>
        ) : cancelado ? (
          <p className="texto-secundario">Evento cancelado: no admite ediciones.</p>
        ) : (
          <p className="texto-secundario">
            Publicado: los datos y los tipos de entrada existentes ya no se editan, pero podés sumar
            entradas o agregar un tipo nuevo.
          </p>
        )}
      </div>

      <div>
        <h3>Datos del evento</h3>
        {errorDatos ? <EstadoError error={errorDatos} /> : null}

        <form className="panel formulario" onSubmit={guardarDatos}>
          <label>
            Nombre
            <input
              required
              maxLength={150}
              value={nombre}
              disabled={!enBorrador}
              onChange={(e) => setNombre(e.target.value)}
            />
          </label>

          <label>
            Descripción <span className="ayuda-campo">(opcional)</span>
            <textarea
              rows={4}
              value={descripcion}
              disabled={!enBorrador}
              onChange={(e) => setDescripcion(e.target.value)}
            />
          </label>

          <label>
            Fecha
            <input
              required
              type="date"
              value={fecha}
              disabled={!enBorrador}
              onChange={(e) => setFecha(e.target.value)}
            />
          </label>

          <label>
            Hora
            <input
              required
              type="time"
              value={hora}
              disabled={!enBorrador}
              onChange={(e) => setHora(e.target.value)}
            />
          </label>

          <label>
            Lugar
            <input
              required
              maxLength={200}
              value={lugar}
              disabled={!enBorrador}
              onChange={(e) => setLugar(e.target.value)}
            />
          </label>

          {enBorrador ? (
            <button className="boton boton-primario" type="submit" disabled={guardandoDatos}>
              {guardandoDatos ? 'Guardando...' : 'Guardar datos'}
            </button>
          ) : null}
        </form>
      </div>

      <div>
        <h3>Tipos de entrada</h3>
        <div className="stack-chico">
          {evento.tiposEntrada.map((tipo) => (
            <FilaTipoEntrada
              key={tipo.id}
              tipo={tipo}
              soloAmpliar={!enBorrador}
              onGuardar={async (cambios) => {
                const actualizado = await editarTipoEntrada(idEventoNumero, tipo.id, cambios);
                setEvento(actualizado);
              }}
              onAmpliar={async (cantidad) => {
                const actualizado = await ampliarCupo(idEventoNumero, tipo.id, { cantidad });
                setEvento(actualizado);
              }}
            />
          ))}
        </div>
      </div>

      {!cancelado ? (
        <div>
          <h3>Agregar tipo de entrada</h3>
          {errorAgregar ? <EstadoError error={errorAgregar} /> : null}

          <form className="panel fila-tipo-entrada" onSubmit={agregarTipo}>
            <label>
              Nombre
              <input
                required
                maxLength={80}
                value={nuevoTipo.nombre}
                onChange={(e) => setNuevoTipo((actual) => ({ ...actual, nombre: e.target.value }))}
              />
            </label>
            <label>
              Precio
              <input
                required
                type="number"
                min={0}
                step="0.01"
                value={nuevoTipo.precio}
                onChange={(e) =>
                  setNuevoTipo((actual) => ({ ...actual, precio: Number(e.target.value) }))
                }
              />
            </label>
            <label>
              Cupo
              <input
                required
                type="number"
                min={1}
                step={1}
                value={nuevoTipo.cupoTotal}
                onChange={(e) =>
                  setNuevoTipo((actual) => ({ ...actual, cupoTotal: Number(e.target.value) }))
                }
              />
            </label>
            <button className="boton boton-primario" type="submit" disabled={agregando}>
              {agregando ? 'Agregando...' : 'Agregar tipo'}
            </button>
          </form>
        </div>
      ) : null}
    </section>
  );
}
