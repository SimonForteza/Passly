import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { EstadoError } from '../layout/EstadoError';
import { EstadoCarga } from '../layout/EstadoCarga';
import { useCambiosSinGuardar } from '../lib/useCambiosSinGuardar';
import { useProductoraActual } from '../productoras/GestionLayout';
import {
  agregarTipoEntrada,
  ampliarCupo,
  editarEvento,
  editarTipoEntrada,
  obtenerEvento,
} from './api';
import type {
  CrearTipoEntradaRequest,
  EventoDTO,
  TipoEntradaDTO,
} from './types';
const INICIAL: CrearTipoEntradaRequest = {
  nombre: '',
  precio: 0,
  cupoTotal: 0,
};
function partirFechaHora(iso: string) {
  const f = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  return {
    fecha:
      f.getFullYear() + '-' + pad(f.getMonth() + 1) + '-' + pad(f.getDate()),
    hora: pad(f.getHours()) + ':' + pad(f.getMinutes()),
  };
}
function FilaTipoEntrada({
  tipo,
  soloAmpliar,
  cancelado,
  guardar,
  ampliar,
  notificarCambios,
}: {
  tipo: TipoEntradaDTO;
  soloAmpliar: boolean;
  cancelado: boolean;
  guardar: (datos: CrearTipoEntradaRequest) => Promise<void>;
  ampliar: (cantidad: number) => Promise<void>;
  notificarCambios: (id: number, cambios: boolean) => void;
}) {
  const [nombre, setNombre] = useState(tipo.nombre);
  const [precio, setPrecio] = useState(tipo.precio);
  const [cupo, setCupo] = useState(tipo.cupoTotal);
  const [sumar, setSumar] = useState(0);
  const [ocupado, setOcupado] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [mensaje, setMensaje] = useState('');
  useEffect(() => {
    setNombre(tipo.nombre);
    setPrecio(tipo.precio);
    setCupo(tipo.cupoTotal);
  }, [tipo.nombre, tipo.precio, tipo.cupoTotal]);
  const editado =
    !soloAmpliar &&
    (nombre.trim() !== tipo.nombre ||
      precio !== tipo.precio ||
      cupo !== tipo.cupoTotal);
  useEffect(() => {
    notificarCambios(tipo.id, editado || sumar > 0);
  }, [tipo.id, editado, sumar, notificarCambios]);
  async function enviar(e: FormEvent, esAmpliacion: boolean) {
    e.preventDefault();
    setOcupado(true);
    setError(null);
    setMensaje('');
    try {
      if (esAmpliacion) {
        await ampliar(sumar);
        setSumar(0);
        setMensaje('Cupo ampliado.');
      } else {
        await guardar({ nombre: nombre.trim(), precio, cupoTotal: cupo });
        setMensaje('Categoría guardada.');
      }
    } catch (causa) {
      setError(causa);
    } finally {
      setOcupado(false);
    }
  }
  return (
    <div className="panel stack-chico">
      <form className="fila-tipo-entrada" onSubmit={(e) => enviar(e, false)}>
        <label>
          Nombre de la categoría
          <input
            required
            maxLength={80}
            value={nombre}
            disabled={soloAmpliar || ocupado}
            onChange={(e) => setNombre(e.target.value)}
          />
        </label>
        <label>
          Precio
          <input
            type="number"
            required
            min={0}
            step="0.01"
            value={precio}
            disabled={soloAmpliar || ocupado}
            onChange={(e) => setPrecio(Number(e.target.value))}
          />
        </label>
        <label>
          Cupo total
          <input
            type="number"
            required
            min={Math.max(1, tipo.cupoTotal - tipo.cupoDisponible)}
            step={1}
            value={cupo}
            disabled={soloAmpliar || ocupado}
            onChange={(e) => setCupo(Number(e.target.value))}
          />
        </label>
        {!soloAmpliar ? (
          <button
            className="boton boton-secundario"
            disabled={ocupado || !editado}
          >
            {ocupado ? 'Guardando…' : 'Guardar esta categoría'}
          </button>
        ) : null}
      </form>
      <p className="ayuda-campo">
        Vendidas: {tipo.cupoTotal - tipo.cupoDisponible} · Disponibles:{' '}
        {tipo.cupoDisponible}
      </p>
      {!cancelado ? (
        <form className="fila-tipo-entrada" onSubmit={(e) => enviar(e, true)}>
          <label>
            Entradas adicionales
            <input
              type="number"
              min={1}
              required
              step={1}
              value={sumar || ''}
              disabled={ocupado}
              placeholder="Cantidad a sumar"
              onChange={(e) => setSumar(Number(e.target.value))}
            />
          </label>
          <button
            className="boton boton-secundario"
            disabled={ocupado || sumar <= 0}
          >
            Sumar cupo
          </button>
        </form>
      ) : null}
      {error ? <EstadoError error={error} /> : null}
      {mensaje ? (
        <p className="mensaje-exito" role="status">
          {mensaje}
        </p>
      ) : null}
    </div>
  );
}
export function EditarEventoPage() {
  const { idEvento } = useParams();
  const p = useProductoraActual();
  const [evento, setEvento] = useState<EventoDTO | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [nombre, setNombre] = useState('');
  const [descripcion, setDescripcion] = useState('');
  const [fecha, setFecha] = useState('');
  const [hora, setHora] = useState('');
  const [lugar, setLugar] = useState('');
  const [ocupado, setOcupado] = useState(false);
  const [mensaje, setMensaje] = useState('');
  const [nuevo, setNuevo] = useState({ ...INICIAL });
  const [cambiosTipos, setCambiosTipos] = useState<Record<number, boolean>>({});
  const notificarCambios = useCallback(
    (id: number, valor: boolean) =>
      setCambiosTipos((actual) =>
        actual[id] === valor ? actual : { ...actual, [id]: valor },
      ),
    [],
  );
  useEffect(() => {
    let vigente = true;
    obtenerEvento(Number(idEvento))
      .then((e) => {
        if (!vigente) return;
        if (e.organizador.id !== p.id) {
          setError(new Error('El evento pertenece a otra productora.'));
          return;
        }
        setEvento(e);
        setNombre(e.nombre);
        setDescripcion(e.descripcion ?? '');
        setLugar(e.lugar);
        const f = partirFechaHora(e.fechaHora);
        setFecha(f.fecha);
        setHora(f.hora);
      })
      .catch((e) => {
        if (vigente) setError(e);
      });
    return () => {
      vigente = false;
    };
  }, [idEvento, p.id]);
  const f = evento ? partirFechaHora(evento.fechaHora) : null;
  const datosModificados =
    !!evento &&
    (nombre.trim() !== evento.nombre ||
      descripcion.trim() !== (evento.descripcion ?? '') ||
      lugar.trim() !== evento.lugar ||
      fecha !== f?.fecha ||
      hora !== f?.hora);
  const nuevoModificado = !!(nuevo.nombre || nuevo.precio || nuevo.cupoTotal);
  useCambiosSinGuardar(
    !ocupado &&
      (datosModificados ||
        nuevoModificado ||
        Object.values(cambiosTipos).some(Boolean)),
  );
  async function guardarDatos(e: FormEvent) {
    e.preventDefault();
    setOcupado(true);
    setError(null);
    setMensaje('');
    try {
      const v = await editarEvento(Number(idEvento), {
        nombre: nombre.trim(),
        descripcion: descripcion.trim(),
        lugar: lugar.trim(),
        fechaHora: new Date(fecha + 'T' + hora).toISOString(),
      });
      setEvento(v);
      setMensaje('Datos del evento guardados.');
    } catch (causa) {
      setError(causa);
    } finally {
      setOcupado(false);
    }
  }
  async function agregar(e: FormEvent) {
    e.preventDefault();
    setOcupado(true);
    setError(null);
    setMensaje('');
    try {
      setEvento(
        await agregarTipoEntrada(Number(idEvento), {
          ...nuevo,
          nombre: nuevo.nombre.trim(),
        }),
      );
      setNuevo({ ...INICIAL });
      setMensaje('Nueva categoría de entradas agregada.');
    } catch (causa) {
      setError(causa);
    } finally {
      setOcupado(false);
    }
  }
  if (!evento && !error) return <EstadoCarga mensaje="Cargando tu evento…" />;
  const base = '/productoras/' + p.id + '/eventos/' + idEvento;
  const borrador = evento?.estado === 'BORRADOR';
  const cancelado = evento?.estado === 'CANCELADO';
  return (
    <section className="stack-grande">
      <Link className="volver" to={base}>
        ← Resumen del evento
      </Link>
      {error ? <EstadoError error={error} /> : null}
      {evento ? (
        <>
          <div>
            <p className="eyebrow">Editar evento</p>
            <h2>{evento.nombre}</h2>
            <p className="texto-secundario">
              {borrador
                ? 'Guardá cada sección por separado. Tus cambios no se publican hasta que publiques el evento.'
                : cancelado
                  ? 'Este evento está cancelado y no admite cambios.'
                  : 'El evento está publicado. Podés sumar cupo y agregar categorías; los datos y las categorías existentes ya no se modifican.'}
            </p>
          </div>
          {mensaje ? (
            <p className="mensaje-exito" role="status">
              {mensaje}
            </p>
          ) : null}
          <form className="panel formulario" onSubmit={guardarDatos}>
            <h3>Datos del evento</h3>
            <label>
              Nombre
              <input
                required
                maxLength={150}
                value={nombre}
                disabled={!borrador || ocupado}
                onChange={(e) => setNombre(e.target.value)}
              />
            </label>
            <label>
              Descripción
              <textarea
                rows={4}
                value={descripcion}
                disabled={!borrador || ocupado}
                onChange={(e) => setDescripcion(e.target.value)}
              />
            </label>
            <label>
              Fecha
              <input
                type="date"
                required
                value={fecha}
                disabled={!borrador || ocupado}
                onChange={(e) => setFecha(e.target.value)}
              />
            </label>
            <label>
              Hora
              <input
                type="time"
                required
                value={hora}
                disabled={!borrador || ocupado}
                onChange={(e) => setHora(e.target.value)}
              />
            </label>
            <label>
              Lugar
              <input
                required
                maxLength={200}
                value={lugar}
                disabled={!borrador || ocupado}
                onChange={(e) => setLugar(e.target.value)}
              />
            </label>
            {borrador ? (
              <button
                className="boton boton-primario"
                disabled={ocupado || !datosModificados}
              >
                {ocupado ? 'Guardando…' : 'Guardar datos del evento'}
              </button>
            ) : null}
          </form>
          <div className="stack-chico">
            <h3>Categorías de entradas</h3>
            {evento.tiposEntrada.map((t) => (
              <FilaTipoEntrada
                key={t.id}
                tipo={t}
                soloAmpliar={!borrador}
                cancelado={!!cancelado}
                notificarCambios={notificarCambios}
                guardar={async (v) => {
                  setEvento(await editarTipoEntrada(evento.id, t.id, v));
                }}
                ampliar={async (cantidad) => {
                  setEvento(await ampliarCupo(evento.id, t.id, { cantidad }));
                }}
              />
            ))}
          </div>
          {!cancelado ? (
            <form className="panel formulario" onSubmit={agregar}>
              <h3>Agregar categoría</h3>
              <label>
                Nombre de la nueva categoría
                <input
                  required
                  maxLength={80}
                  value={nuevo.nombre}
                  onChange={(e) =>
                    setNuevo({ ...nuevo, nombre: e.target.value })
                  }
                />
              </label>
              <label>
                Precio
                <input
                  required
                  type="number"
                  min={0}
                  step="0.01"
                  value={nuevo.precio}
                  onChange={(e) =>
                    setNuevo({ ...nuevo, precio: Number(e.target.value) })
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
                  value={nuevo.cupoTotal || ''}
                  onChange={(e) =>
                    setNuevo({ ...nuevo, cupoTotal: Number(e.target.value) })
                  }
                />
              </label>
              <button className="boton boton-primario" disabled={ocupado}>
                {ocupado ? 'Agregando…' : 'Agregar categoría'}
              </button>
            </form>
          ) : null}
        </>
      ) : null}
    </section>
  );
}
