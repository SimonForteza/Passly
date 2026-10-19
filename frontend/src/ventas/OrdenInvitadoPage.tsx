import { useEffect, useState, type FormEvent } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { EstadoError } from '../layout/EstadoError';
import {
  consultarInvitado,
  recuperarInvitado,
  type DetalleInvitado,
} from './invitadosApi';
import { formatearFechaOrden, formatearPrecio } from './formato';
import { EntradasDeOrden } from './EntradasDeOrden';
export function OrdenInvitadoPage() {
  const token = useLocation().hash.slice(1);
  const [resultado, setResultado] = useState<{
    token: string;
    valor: DetalleInvitado;
  } | null>(null);
  const [fallo, setFallo] = useState<{ token: string; error: unknown } | null>(
    null,
  );
  const detalle = resultado?.token === token ? resultado.valor : null;
  const error = fallo?.token === token ? fallo.error : null;
  const [orden, setOrden] = useState('');
  const [email, setEmail] = useState('');
  const [mensaje, setMensaje] = useState('');
  const [ocupado, setOcupado] = useState(false);
  useEffect(() => {
    let vigente = true;
    if (token)
      consultarInvitado(token)
        .then((v) => {
          if (vigente) setResultado({ token, valor: v });
        })
        .catch((e) => {
          if (vigente) setFallo({ token, error: e });
        });
    return () => {
      vigente = false;
    };
  }, [token]);
  async function recuperar(e: FormEvent) {
    e.preventDefault();
    setOcupado(true);
    setFallo(null);
    setMensaje('');
    try {
      await recuperarInvitado(Number(orden), email.trim());
      setMensaje(
        'Si los datos coinciden con una compra vigente, recibirás un nuevo enlace por email. Revisá también la carpeta de correo no deseado. Si se emite un enlace nuevo, el anterior deja de funcionar.',
      );
    } catch (e) {
      setFallo({ token, error: e });
    } finally {
      setOcupado(false);
    }
  }
  return (
    <section className="stack-grande">
      <div>
        <p className="eyebrow">Tus entradas</p>
        <h2>{detalle ? 'Compra confirmada' : 'Recuperar mis entradas'}</h2>
      </div>
      {error ? (
        <>
          <EstadoError error={error} />
          <p>
            Si el enlace venció o fue reemplazado, solicitá uno nuevo con los
            datos de tu compra.
          </p>
        </>
      ) : null}
      {token && !detalle && !error ? <p>Cargando entradas…</p> : null}
      {detalle ? (
        <>
          <p>
            Orden #{detalle.orden.id} ·{' '}
            {formatearFechaOrden(detalle.orden.creadaEn)} · Total{' '}
            {formatearPrecio(detalle.orden.total)}
          </p>
          <p>
            A nombre de {detalle.orden.comprador.nombre} ·{' '}
            {detalle.orden.comprador.email}
          </p>
          <p className="aviso-informativo">
            El acceso también se envía a tu email. Si no llega, podés solicitar
            otro enlace. Guardá este enlace privado: quien lo tenga puede ver
            tus QR. Compartí cada QR solo con la persona que vaya a usarlo.
          </p>
          <EntradasDeOrden orden={detalle.orden} tickets={detalle.tickets} />
          <div className="acciones">
            <Link className="boton boton-secundario" to="/">
              Ver otros eventos
            </Link>
            <Link className="volver" to="/orden-invitado">
              Solicitar otro enlace
            </Link>
          </div>
        </>
      ) : !token || error ? (
        <form className="panel formulario" onSubmit={recuperar}>
          <p>
            Usá el email y el número de orden de tu compra sin cuenta. El número
            aparece en la confirmación y en el correo que recibiste.
          </p>
          <label>
            Número de orden
            <input
              required
              type="number"
              min={1}
              value={orden}
              onChange={(e) => setOrden(e.target.value)}
            />
          </label>
          <label>
            Email de la compra
            <input
              required
              type="email"
              autoComplete="email"
              maxLength={254}
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </label>
          <button className="boton boton-primario" disabled={ocupado}>
            {ocupado ? 'Solicitando…' : 'Enviar nuevo enlace'}
          </button>
          {mensaje ? (
            <p role="status" className="mensaje-exito">
              {mensaje}
            </p>
          ) : null}
          <Link className="volver" to="/mis-ordenes">
            Compré con mi cuenta: ver mis entradas
          </Link>
        </form>
      ) : null}
    </section>
  );
}
