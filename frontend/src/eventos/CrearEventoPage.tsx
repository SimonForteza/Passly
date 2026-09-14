import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { EstadoError } from '../layout/EstadoError';
import { useAuth } from '../lib/auth/AuthContext';
import { crearEvento } from './api';
import type { CrearTipoEntradaRequest } from './types';

const TIPO_ENTRADA_INICIAL: CrearTipoEntradaRequest = { nombre: '', precio: 0, cupoTotal: 0 };

export function CrearEventoPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { usuario } = useAuth();
  const idProductora = Number(id);
  const idValido = Number.isInteger(idProductora) && idProductora > 0;

  const [nombre, setNombre] = useState('');
  const [descripcion, setDescripcion] = useState('');
  const [fecha, setFecha] = useState('');
  const [hora, setHora] = useState('');
  const [lugar, setLugar] = useState('');
  const [tiposEntrada, setTiposEntrada] = useState<CrearTipoEntradaRequest[]>([{ ...TIPO_ENTRADA_INICIAL }]);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState<unknown>(null);

  const puedeCrear = usuario?.rol === 'ORGANIZADOR';

  function actualizarTipo(indice: number, cambios: Partial<CrearTipoEntradaRequest>) {
    setTiposEntrada((actuales) =>
      actuales.map((tipo, i) => (i === indice ? { ...tipo, ...cambios } : tipo)),
    );
  }

  function agregarTipo() {
    setTiposEntrada((actuales) => [...actuales, { ...TIPO_ENTRADA_INICIAL }]);
  }

  function eliminarTipo(indice: number) {
    setTiposEntrada((actuales) => actuales.filter((_tipo, i) => i !== indice));
  }

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    setGuardando(true);
    setError(null);

    try {
      const fechaHora = new Date(`${fecha}T${hora}`).toISOString();
      const creado = await crearEvento({
        idProductora,
        nombre: nombre.trim(),
        descripcion: descripcion.trim(),
        fechaHora,
        lugar: lugar.trim(),
        tiposEntrada,
      });
      navigate(`/eventos/${creado.id}`);
    } catch (causa: unknown) {
      setError(causa);
    } finally {
      setGuardando(false);
    }
  }

  if (!idValido) {
    return (
      <section className="stack-grande">
        <Link className="volver" to="/productoras">← Mis productoras</Link>
        <p className="mensaje-error" role="alert">El identificador de la productora no es válido.</p>
      </section>
    );
  }

  if (!puedeCrear) {
    return (
      <section className="stack-grande">
        <Link className="volver" to={`/productoras/${idProductora}/eventos`}>← Eventos de la productora</Link>
        <p className="mensaje-error" role="alert">
          Necesitás el rol ORGANIZADOR para crear un evento.
        </p>
      </section>
    );
  }

  return (
    <section className="stack-grande">
      <Link className="volver" to={`/productoras/${idProductora}/eventos`}>← Eventos de la productora</Link>

      <div>
        <p className="eyebrow">Nuevo evento</p>
        <h2>Crear evento</h2>
        <p className="texto-secundario">Nace en BORRADOR: se publica después desde el backoffice.</p>
      </div>

      {error ? <EstadoError error={error} /> : null}

      <form className="panel formulario" onSubmit={enviar}>
        <label>
          Nombre
          <input required maxLength={150} value={nombre} onChange={(e) => setNombre(e.target.value)} />
        </label>

        <label>
          Descripción <span className="ayuda-campo">(opcional)</span>
          <textarea rows={4} value={descripcion} onChange={(e) => setDescripcion(e.target.value)} />
        </label>

        <label>
          Fecha
          <input required type="date" value={fecha} onChange={(e) => setFecha(e.target.value)} />
        </label>

        <label>
          Hora
          <input required type="time" value={hora} onChange={(e) => setHora(e.target.value)} />
        </label>

        <label>
          Lugar
          <input required maxLength={200} value={lugar} onChange={(e) => setLugar(e.target.value)} />
        </label>

        <div>
          <div className="titulo-seccion">
            <div>
              <p className="eyebrow">Entradas</p>
              <h3>Tipos de entrada</h3>
            </div>
            <button className="boton boton-secundario" type="button" onClick={agregarTipo}>
              Agregar tipo
            </button>
          </div>

          <div className="stack-chico">
            {tiposEntrada.map((tipo, indice) => (
              <div className="fila-tipo-entrada" key={indice}>
                <label>
                  Nombre
                  <input
                    required
                    maxLength={80}
                    value={tipo.nombre}
                    onChange={(e) => actualizarTipo(indice, { nombre: e.target.value })}
                  />
                </label>
                <label>
                  Precio
                  <input
                    required
                    type="number"
                    min={0}
                    step="0.01"
                    value={tipo.precio}
                    onChange={(e) => actualizarTipo(indice, { precio: Number(e.target.value) })}
                  />
                </label>
                <label>
                  Cupo
                  <input
                    required
                    type="number"
                    min={1}
                    step={1}
                    value={tipo.cupoTotal}
                    onChange={(e) => actualizarTipo(indice, { cupoTotal: Number(e.target.value) })}
                  />
                </label>
                <button
                  className="boton boton-secundario"
                  type="button"
                  onClick={() => eliminarTipo(indice)}
                  disabled={tiposEntrada.length === 1}
                >
                  Eliminar
                </button>
              </div>
            ))}
          </div>
        </div>

        <button className="boton boton-primario" type="submit" disabled={guardando}>
          {guardando ? 'Creando...' : 'Crear evento'}
        </button>
      </form>
    </section>
  );
}
