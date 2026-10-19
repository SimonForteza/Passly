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
import type { CarritoDTO } from './types';
export function CarritoInvitadoPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const [carrito, setCarrito] = useState<CarritoDTO | null>(null);
  const [recibidoEn, setRecibidoEn] = useState(() => Date.now());
  const [error, setError] = useState<unknown>(null);
  const [ocupado, setOcupado] = useState(false);
  const [nombre, setNombre] = useState('');
  const [email, setEmail] = useState('');
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
  async function pagar(e: FormEvent) {
    e.preventDefault();
    setOcupado(true);
    setError(null);
    const intento = clave || crypto.randomUUID();
    setClave(intento);
    sessionStorage.setItem('passly.intentoInvitado', intento);
    try {
      const compra = await comprarInvitado(
        nombre.trim(),
        email.trim(),
        intento,
      );
      sessionStorage.removeItem('passly.intentoInvitado');
      actualizarCarrito(null);
      navigate('/orden-invitado#' + compra.token, { replace: true });
    } catch (causa) {
      setError(causa);
      if (
        causa instanceof ApiError &&
        (causa.status === 402 ||
          causa.detail.startsWith('El cobro fue revertido.'))
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
    sessionStorage.removeItem('passly.intentoInvitado');
    setClave('');
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
                  onChange={(e) => setNombre(e.target.value)}
                  disabled={ocupado}
                />
              </label>
              <label>
                Email
                <input
                  required
                  type="email"
                  maxLength={254}
                  autoComplete="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  disabled={ocupado}
                />
              </label>
              <p>
                Revisá el email: ahí vas a recibir el acceso a tus entradas.
              </p>
              <p className="aviso-informativo">
                <strong>Compra de prueba.</strong> No se solicitarán datos de
                tarjeta ni se realizará un cobro real.
              </p>
              <button
                className="boton boton-primario"
                disabled={ocupado || cuenta.vencido}
              >
                {ocupado
                  ? 'Procesando…'
                  : 'Confirmar compra de prueba · ' +
                    formatearPrecio(carrito.total)}
              </button>
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
              <button
                type="button"
                className="boton-texto"
                disabled={ocupado}
                onClick={vaciar}
              >
                Vaciar carrito
              </button>
            </form>
            <ResumenDeCarrito carrito={carrito} />
          </div>
          {cuenta.vencido ? (
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
