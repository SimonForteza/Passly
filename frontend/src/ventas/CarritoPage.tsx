import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { EstadoCarga } from '../layout/EstadoCarga';
import { EstadoError } from '../layout/EstadoError';
import { useAuth } from '../lib/auth/AuthContext';
import { verCarritoInvitado, transferirCarritoInvitado } from './invitadosApi';
import { confirmarCompra, obtenerCarrito, vaciarCarrito } from './api';
import { formatearPrecio } from './formato';
import { ModalCarritoVencido } from './ModalCarritoVencido';
import { ResumenDeCarrito } from './ResumenDeCarrito';
import type { CarritoDTO } from './types';
import { useCuentaRegresiva } from './useCuentaRegresiva';
import { useEventos } from './useEventos';
export function CarritoPage() {
  const { usuario } = useAuth();
  const navigate = useNavigate();
  const [carrito, setCarrito] = useState<CarritoDTO | null>(null);
  const [recibidoEn, setRecibidoEn] = useState(() => Date.now());
  const [error, setError] = useState<unknown>(null);
  const [procesando, setProcesando] = useState(false);
  const cuenta = useCuentaRegresiva(carrito, recibidoEn);
  const eventos = useEventos(carrito?.items.map((i) => i.idEvento) ?? []);
  useEffect(() => {
    let vigente = true;
    verCarritoInvitado()
      .then((i) =>
        i.items.length ? transferirCarritoInvitado() : obtenerCarrito(),
      )
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
  async function pagar() {
    setProcesando(true);
    setError(null);
    try {
      const orden = await confirmarCompra();
      await vaciarCarrito().catch(() => undefined);
      navigate('/mis-ordenes/' + orden.id, {
        replace: true,
        state: { recienConfirmada: true },
      });
    } catch (causa) {
      setError(causa);
      await obtenerCarrito()
        .then((v) => {
          setCarrito(v);
          setRecibidoEn(Date.now());
        })
        .catch(() => undefined);
      setProcesando(false);
    }
  }
  async function vaciar() {
    setProcesando(true);
    setError(null);
    try {
      await vaciarCarrito();
      navigate('/');
    } catch (e) {
      setError(e);
      setProcesando(false);
    }
  }
  if (!carrito && !error) return <EstadoCarga mensaje="Cargando tu carrito…" />;
  return (
    <section className="stack-grande">
      <div>
        <p className="eyebrow">Tus entradas</p>
        <h2>Tu carrito</h2>
      </div>
      {error ? <EstadoError error={error} /> : null}
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
            <div className="panel formulario">
              <h3>Tus datos</h3>
              <dl>
                <dt>Nombre</dt>
                <dd>{usuario?.nombre}</dd>
                <dt>Email</dt>
                <dd>{usuario?.email}</dd>
              </dl>
              <p>
                Las entradas quedarán disponibles en Mis entradas, dentro de tu
                cuenta.
              </p>
              <p className="aviso-informativo">
                <strong>Compra de prueba.</strong> No se solicitarán datos de
                tarjeta ni se realizará un cobro real.
              </p>
              <button
                className="boton boton-primario"
                onClick={pagar}
                disabled={procesando || cuenta.vencido}
              >
                {procesando
                  ? 'Procesando…'
                  : 'Confirmar compra de prueba · ' +
                    formatearPrecio(carrito.total)}
              </button>
              <button
                className="boton-texto"
                onClick={vaciar}
                disabled={procesando}
              >
                Vaciar carrito
              </button>
            </div>
            <ResumenDeCarrito carrito={carrito} />
          </div>
          {cuenta.vencido ? (
            <ModalCarritoVencido carrito={carrito} eventos={eventos} />
          ) : null}
        </>
      ) : null}
    </section>
  );
}
