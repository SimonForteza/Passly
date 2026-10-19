import { useEffect, useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { EstadoError } from '../layout/EstadoError';
import { ApiError } from '../lib/http/ApiError';
import {
  verCarritoInvitado,
  vaciarInvitado,
  comprarInvitado,
} from './invitadosApi';
import { formatearPrecio } from './formato';
import { actualizarCarrito } from './carritoEstado';
import { useCuentaRegresiva } from './useCuentaRegresiva';
import { useEventos } from './useEventos';
import { ResumenDeCarrito } from './ResumenDeCarrito';
import { ModalCarritoVencido } from './ModalCarritoVencido';
import { FormularioTarjeta } from './FormularioTarjeta';
import { digitosDeNumero, quitarPrefijoTarjeta, TARJETA_VACIA, validarTarjeta } from './tarjeta';
import type { CarritoDTO, DatosDeTarjetaDTO } from './types';

function mensajeDePago(causa: unknown): string | null {
  if (!(causa instanceof ApiError)) return null;
  if (causa.status === 402) return 'El pago fue rechazado. Revisá los datos o probá con otra tarjeta.';
  if (causa.status === 503) return 'No pudimos confirmar el resultado del pago. No vuelvas a intentarlo hasta que verifiquemos qué pasó.';
  return null;
}

export function CarritoInvitadoPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const [carrito, setCarrito] = useState<CarritoDTO | null>(null);
  const [recibidoEn, setRecibidoEn] = useState(() => Date.now());
  const [error, setError] = useState<unknown>(null);
  const [errorDePago, setErrorDePago] = useState<unknown>(null);
  const [pagoEnRevision, setPagoEnRevision] = useState(false);
  const [ocupado, setOcupado] = useState(false);
  const [nombre, setNombre] = useState('');
  const [email, setEmail] = useState('');
  const [tarjeta, setTarjeta] = useState(TARJETA_VACIA);
  const [erroresTarjeta, setErroresTarjeta] = useState<Record<string, string>>({});
  const [erroresDatos, setErroresDatos] = useState<Record<string, string>>({});
  const [clave, setClave] = useState(
    () => sessionStorage.getItem('passly.intentoInvitado') ?? '',
  );
  const eventos = useEventos(carrito?.items.map((i) => i.idEvento) ?? []);
  const cuenta = useCuentaRegresiva(carrito, recibidoEn);

  useEffect(() => {
    let vigente = true;
    verCarritoInvitado()
      .then((v) => {
        if (vigente) {
          setCarrito(v);
          setRecibidoEn(Date.now());
        }
      })
      .catch((e) => {
        if (vigente) setError(e);
      });
    return () => {
      vigente = false;
    };
  }, []);

  function cambiarTarjeta(nueva: DatosDeTarjetaDTO) {
    setTarjeta(nueva);
    if (Object.keys(erroresTarjeta).length > 0) {
      setErroresTarjeta(validarTarjeta(nueva));
    }
  }

  async function pagar(e: FormEvent) {
    e.preventDefault();
    const erroresDeValidacion = validarTarjeta(tarjeta);
    if (Object.keys(erroresDeValidacion).length > 0) {
      setErroresTarjeta(erroresDeValidacion);
      return;
    }

    setErroresTarjeta({});
    setOcupado(true);
    setError(null);
    setErrorDePago(null);
    setPagoEnRevision(false);
    const intento = clave || crypto.randomUUID();
    setClave(intento);
    sessionStorage.setItem('passly.intentoInvitado', intento);
    try {
      const compra = await comprarInvitado(
        nombre.trim(),
        email.trim(),
        intento,
        { ...tarjeta, numero: digitosDeNumero(tarjeta.numero) },
      );
      setTarjeta(TARJETA_VACIA);
      sessionStorage.removeItem('passly.intentoInvitado');
      actualizarCarrito(null);
      navigate('/orden-invitado#' + compra.token, { replace: true });
    } catch (causa) {
      if (causa instanceof ApiError && causa.status === 400 && causa.errores) {
        setErroresTarjeta(quitarPrefijoTarjeta(causa.errores));
        setErroresDatos({
          nombre: causa.errores.nombre ?? '',
          email: causa.errores.email ?? '',
        });
      } else if (causa instanceof ApiError && (causa.status === 402 || causa.status === 503)) {
        setErrorDePago(causa);
        setPagoEnRevision(causa.status === 503);
      } else {
        setError(causa);
      }
      if (
        causa instanceof ApiError &&
        (causa.status === 402 || causa.detail.startsWith('El cobro fue revertido.'))
      ) {
        setClave('');
        sessionStorage.removeItem('passly.intentoInvitado');
      }
      await verCarritoInvitado()
        .then((v) => {
          setCarrito(v);
          setRecibidoEn(Date.now());
        })
        .catch(() => undefined);
    } finally {
      setOcupado(false);
    }
  }

  async function vaciarSeleccion() {
    await vaciarInvitado();
    setCarrito((actual) => (actual ? { ...actual, items: [], total: 0 } : actual));
  }

  async function vaciar() {
    setOcupado(true);
    setError(null);
    try {
      await vaciarSeleccion();
      navigate('/');
    } catch (e) {
      setError(e);
    } finally {
      setOcupado(false);
    }
  }

  return (
    <section className="stack-grande">
      <div>
        <p className="eyebrow">Tus entradas</p>
        <h2>Tu carrito</h2>
        <p>
          Comprá sin crear una cuenta. Te enviaremos un enlace privado para
          consultar y descargar tus QR.
        </p>
      </div>
      {errorDePago ? (
        mensajeDePago(errorDePago) ? (
          <p className="mensaje-error" role="alert">{mensajeDePago(errorDePago)}</p>
        ) : (
          <EstadoError error={errorDePago} />
        )
      ) : null}
      {error ? <EstadoError error={error} /> : null}
      {!carrito && !error ? <p>Cargando carrito…</p> : null}
      {carrito && !carrito.items.length ? (
        <div className="panel estado-vacio">
          <h3>Tu carrito está vacío</h3>
          <p>Elegí un evento para empezar.</p>
          <Link className="boton boton-primario" to="/">
            Ver eventos
          </Link>
        </div>
      ) : null}
      {carrito && carrito.items.length > 0 ? (
        <>
          <p className="barra-hold" role="timer">
            Tu selección vence en <strong>{cuenta.texto}</strong>. El cupo se
            confirma al pagar.
          </p>
          <div className="checkout">
            <form className="panel formulario" onSubmit={pagar}>
              <h3>Continuar como invitado</h3>
              <label>
                Nombre
                <input
                  required
                  maxLength={150}
                  autoComplete="name"
                  value={nombre}
                  onChange={(e) => {
                    setNombre(e.target.value);
                    setErroresDatos((actuales) => ({ ...actuales, nombre: '' }));
                  }}
                  disabled={ocupado || pagoEnRevision}
                  aria-invalid={Boolean(erroresDatos.nombre)}
                  aria-describedby={erroresDatos.nombre ? 'invitado-nombre-error' : undefined}
                />
                {erroresDatos.nombre ? <span id="invitado-nombre-error" className="ayuda-campo-error" role="alert">{erroresDatos.nombre}</span> : null}
              </label>
              <label>
                Email
                <input
                  required
                  type="email"
                  maxLength={254}
                  autoComplete="email"
                  value={email}
                  onChange={(e) => {
                    setEmail(e.target.value);
                    setErroresDatos((actuales) => ({ ...actuales, email: '' }));
                  }}
                  disabled={ocupado || pagoEnRevision}
                  aria-invalid={Boolean(erroresDatos.email)}
                  aria-describedby={erroresDatos.email ? 'invitado-email-error' : undefined}
                />
                {erroresDatos.email ? <span id="invitado-email-error" className="ayuda-campo-error" role="alert">{erroresDatos.email}</span> : null}
              </label>
              <p>Revisá el email: ahí vas a recibir el acceso a tus entradas.</p>
              <h3>Medio de pago</h3>
              <p className="opcion-pago-detalle">
                Pasarela de prueba: <strong>4242 4242 4242 4242</strong> aprueba,{' '}
                <strong>4000 0000 0000 0002</strong> rechaza, <strong>4000 0000 0000 0119</strong>{' '}
                simula que la pasarela no responde.
              </p>
              <FormularioTarjeta
                tarjeta={tarjeta}
                onCambio={cambiarTarjeta}
                erroresCampo={erroresTarjeta}
                deshabilitado={ocupado || pagoEnRevision}
              />
              <p className="aviso-pasarela">
                El pago se procesa con la pasarela externa en modo de prueba.
              </p>
              <button
                className="boton boton-primario"
                disabled={ocupado || cuenta.vencido || pagoEnRevision}
              >
                {ocupado
                  ? 'Procesando…'
                  : pagoEnRevision
                    ? 'Pago en revisión'
                    : `Pagar ${formatearPrecio(carrito.total)}`}
              </button>
              {!pagoEnRevision ? (
                <Link
                  className="boton boton-secundario"
                  to="/login"
                  state={{
                    from: location,
                    contexto: 'Ingresá para continuar con tu selección',
                  }}
                >
                  Ingresar y continuar con mi cuenta
                </Link>
              ) : null}
              <button
                type="button"
                className="boton-texto"
                disabled={ocupado || pagoEnRevision}
                onClick={vaciar}
              >
                Vaciar carrito
              </button>
            </form>
            <ResumenDeCarrito carrito={carrito} />
          </div>
          {cuenta.vencido && !pagoEnRevision ? (
            <ModalCarritoVencido
              carrito={carrito}
              eventos={eventos}
              vaciarSeleccion={vaciarSeleccion}
            />
          ) : null}
        </>
      ) : null}
    </section>
  );
}
