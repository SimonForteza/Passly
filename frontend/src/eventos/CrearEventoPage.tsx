import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { EstadoError } from '../layout/EstadoError';
import { useAuth } from '../lib/auth/AuthContext';
import { crearEvento, subirPortada } from './api';
import { CampoPortada } from './CampoPortada';
import { errorDePortada, useObjectUrl } from './portada';
import { mensajeDeError } from '../lib/http/ApiError';
import type { CrearTipoEntradaRequest } from './types';
import { useProductoraActual } from '../productoras/GestionLayout';
import { useCambiosSinGuardar } from '../lib/useCambiosSinGuardar';

const TIPO_ENTRADA_INICIAL: CrearTipoEntradaRequest = {
  nombre: '',
  precio: 0,
  cupoTotal: 0,
};

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
  const [tiposEntrada, setTiposEntrada] = useState<CrearTipoEntradaRequest[]>([
    { ...TIPO_ENTRADA_INICIAL },
  ]);
  // La portada se elige acá pero se sube recién cuando el evento existe (necesita su id).
  const [portada, setPortada] = useState<File | null>(null);
  const [errorPortada, setErrorPortada] = useState<string | null>(null);
  const vistaPortada = useObjectUrl(portada);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState<unknown>(null);

  const productora = useProductoraActual();
  const puedeCrear =
    productora.puedeGestionar &&
    (usuario?.rol === 'ORGANIZADOR' || usuario?.rol === 'ADMIN');
  useCambiosSinGuardar(
    !guardando &&
      !!(
        nombre ||
        descripcion ||
        fecha ||
        hora ||
        lugar ||
        portada ||
        tiposEntrada.some((t) => t.nombre || t.precio || t.cupoTotal)
      ),
  );

  function actualizarTipo(
    indice: number,
    cambios: Partial<CrearTipoEntradaRequest>,
  ) {
    setTiposEntrada((actuales) =>
      actuales.map((tipo, i) =>
        i === indice ? { ...tipo, ...cambios } : tipo,
      ),
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
      // Si la portada falla, el evento igual quedó creado: se avisa en su gestión, donde se puede
      // volver a subir, en vez de dejar a la persona en un formulario que ya se guardó.
      let falloPortada: string | null = null;
      if (portada) {
        try {
          await subirPortada(creado.id, portada);
        } catch (causa: unknown) {
          falloPortada = mensajeDeError(causa);
        }
      }
      navigate(`/productoras/${idProductora}/eventos/${creado.id}`, {
        replace: true,
        state: { eventoCreado: true, falloPortada },
      });
    } catch (causa: unknown) {
      setError(causa);
    } finally {
      setGuardando(false);
    }
  }

  if (!idValido) {
    return (
      <section className="stack-grande">
        <Link className="volver" to="/productoras">
          ← Mis productoras
        </Link>
        <p className="mensaje-error" role="alert">
          El identificador de la productora no es válido.
        </p>
      </section>
    );
  }

  if (!puedeCrear) {
    return (
      <section className="stack-grande">
        <Link className="volver" to={`/productoras/${idProductora}/eventos`}>
          ← Eventos de la productora
        </Link>
        <p className="mensaje-error" role="alert">
          Necesitás el rol ORGANIZADOR para crear un evento.
        </p>
      </section>
    );
  }

  return (
    <section className="stack-grande">
      <Link className="volver" to={`/productoras/${idProductora}/eventos`}>
        ← Eventos de la productora
      </Link>

      <div>
        <p className="eyebrow">Nuevo evento</p>
        <h2>Crear evento</h2>
        <p className="texto-secundario">
          Se guardará como borrador en {productora.nombreComercial}. Podrás
          revisarlo antes de publicarlo.
        </p>
      </div>

      {error ? <EstadoError error={error} /> : null}

      <form className="panel formulario" onSubmit={enviar}>
        <label>
          Nombre
          <input
            required
            maxLength={150}
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
          />
        </label>

        <label>
          Descripción <span className="ayuda-campo">(opcional)</span>
          <textarea
            rows={4}
            value={descripcion}
            onChange={(e) => setDescripcion(e.target.value)}
          />
        </label>

        <label>
          Fecha
          <input
            required
            type="date"
            value={fecha}
            onChange={(e) => setFecha(e.target.value)}
          />
        </label>

        <label>
          Hora
          <input
            required
            type="time"
            value={hora}
            onChange={(e) => setHora(e.target.value)}
          />
        </label>

        <label>
          Lugar
          <input
            required
            maxLength={200}
            value={lugar}
            onChange={(e) => setLugar(e.target.value)}
          />
        </label>

        <CampoPortada
          vistaPrevia={vistaPortada}
          nombreArchivo={portada?.name}
          pesoArchivo={portada?.size}
          ocupado={guardando}
          error={errorPortada}
          onElegir={(archivo) => {
            const invalido = errorDePortada(archivo);
            setErrorPortada(invalido);
            if (!invalido) setPortada(archivo);
          }}
          onQuitar={() => setPortada(null)}
        />

        <div>
          <div className="titulo-seccion">
            <div>
              <p className="eyebrow">Entradas</p>
              <h3>Tipos de entrada</h3>
            </div>
            <button
              className="boton boton-secundario"
              type="button"
              onClick={agregarTipo}
            >
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
                    onChange={(e) =>
                      actualizarTipo(indice, { nombre: e.target.value })
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
                    value={tipo.precio}
                    onChange={(e) =>
                      actualizarTipo(indice, { precio: Number(e.target.value) })
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
                    value={tipo.cupoTotal}
                    onChange={(e) =>
                      actualizarTipo(indice, {
                        cupoTotal: Number(e.target.value),
                      })
                    }
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

        <button
          className="boton boton-primario"
          type="submit"
          disabled={guardando}
        >
          {guardando ? 'Guardando borrador…' : 'Guardar borrador'}
        </button>
      </form>
    </section>
  );
}
