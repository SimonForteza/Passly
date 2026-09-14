import { useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { EstadoCarga } from '../layout/EstadoCarga';
import { EstadoError } from '../layout/EstadoError';
import { useAuth } from '../lib/auth/AuthContext';
import { crearProductora, misProductoras } from './api';
import type { CrearProductoraRequest, ProductoraDTO } from './types';

const FORMULARIO_INICIAL: CrearProductoraRequest = {
  nombreComercial: '',
  cuit: '',
  descripcion: '',
  logoUrl: '',
};

export function ProductorasPage() {
  const { usuario } = useAuth();
  const [productoras, setProductoras] = useState<ProductoraDTO[]>([]);
  const [formulario, setFormulario] = useState(FORMULARIO_INICIAL);
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [mensaje, setMensaje] = useState('');

  useEffect(() => {
    let vigente = true;

    misProductoras()
      .then((resultado) => {
        if (vigente) setProductoras(resultado);
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
  }, []);

  async function enviarAlta(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    setGuardando(true);
    setError(null);
    setMensaje('');

    try {
      const creada = await crearProductora({
        nombreComercial: formulario.nombreComercial.trim(),
        cuit: formulario.cuit?.trim() || undefined,
        descripcion: formulario.descripcion?.trim() || undefined,
        logoUrl: formulario.logoUrl?.trim() || undefined,
      });
      setProductoras((actuales) =>
        [...actuales, creada].sort((a, b) => a.nombreComercial.localeCompare(b.nombreComercial)),
      );
      setFormulario(FORMULARIO_INICIAL);
      setMensaje(`${creada.nombreComercial} fue creada correctamente.`);
    } catch (causa: unknown) {
      setError(causa);
    } finally {
      setGuardando(false);
    }
  }

  const puedeCrear = usuario?.rol === 'ORGANIZADOR';

  return (
    <section className="stack-grande">
      <div className="encabezado-pagina">
        <div>
          <p className="eyebrow">Gestión</p>
          <h2>Mis productoras</h2>
          <p className="texto-secundario">
            Administrá las organizaciones de las que formás parte y sus equipos.
          </p>
        </div>
      </div>

      {error ? <EstadoError error={error} /> : null}
      {mensaje ? <p className="mensaje-exito" role="status">{mensaje}</p> : null}

      {cargando ? (
        <EstadoCarga mensaje="Cargando tus productoras..." />
      ) : productoras.length === 0 ? (
        <div className="panel estado-vacio">
          <h3>Todavía no pertenecés a una productora</h3>
          <p className="texto-secundario">
            {puedeCrear
              ? 'Creá la primera con el formulario de abajo.'
              : 'Un dueño debe incorporarte a su padrón.'}
          </p>
        </div>
      ) : (
        <div className="grilla-tarjetas">
          {productoras.map((productora) => (
            <article className="panel tarjeta-productora" key={productora.id}>
              {productora.logoUrl ? (
                <img src={productora.logoUrl} alt={`Logo de ${productora.nombreComercial}`} />
              ) : (
                <div className="logo-placeholder" aria-hidden="true">
                  {productora.nombreComercial.slice(0, 1).toUpperCase()}
                </div>
              )}
              <div>
                <h3>{productora.nombreComercial}</h3>
                <p className="texto-secundario">
                  {productora.descripcion || 'Sin descripción cargada.'}
                </p>
              </div>
              <Link className="boton boton-secundario" to={`/productoras/${productora.id}/backoffice`}>
                Ver padrón
              </Link>
            </article>
          ))}
        </div>
      )}

      {puedeCrear ? (
        <form className="panel formulario" onSubmit={enviarAlta}>
          <div>
            <p className="eyebrow">Nueva organización</p>
            <h3>Crear productora</h3>
          </div>

          <label>
            Nombre comercial
            <input
              required
              maxLength={150}
              value={formulario.nombreComercial}
              onChange={(evento) =>
                setFormulario((actual) => ({ ...actual, nombreComercial: evento.target.value }))
              }
            />
          </label>

          <label>
            CUIT <span className="ayuda-campo">(opcional, 11 dígitos)</span>
            <input
              inputMode="numeric"
              pattern="[0-9]{11}"
              maxLength={11}
              value={formulario.cuit}
              onChange={(evento) =>
                setFormulario((actual) => ({ ...actual, cuit: evento.target.value }))
              }
            />
          </label>

          <label>
            Descripción <span className="ayuda-campo">(opcional)</span>
            <textarea
              rows={4}
              maxLength={2000}
              value={formulario.descripcion}
              onChange={(evento) =>
                setFormulario((actual) => ({ ...actual, descripcion: evento.target.value }))
              }
            />
          </label>

          <label>
            URL del logo <span className="ayuda-campo">(opcional)</span>
            <input
              type="url"
              maxLength={500}
              placeholder="https://..."
              value={formulario.logoUrl}
              onChange={(evento) =>
                setFormulario((actual) => ({ ...actual, logoUrl: evento.target.value }))
              }
            />
          </label>

          <button className="boton boton-primario" type="submit" disabled={guardando}>
            {guardando ? 'Creando...' : 'Crear productora'}
          </button>
        </form>
      ) : null}
    </section>
  );
}
