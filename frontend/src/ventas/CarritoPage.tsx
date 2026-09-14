import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { EstadoCarga } from '../layout/EstadoCarga';
import { EstadoError } from '../layout/EstadoError';
import { useAuth } from '../lib/auth/AuthContext';
import { confirmarCompra, obtenerCarrito, vaciarCarrito } from './api';
import { formatearFechaEvento, formatearPrecio } from './formato';
import iconoCandado from './iconos/candado.svg';
import iconoCheckPaso from './iconos/check-paso.svg';
import iconoRadio from './iconos/radio-seleccionado.svg';
import iconoReloj from './iconos/reloj.svg';
import iconoTarjeta from './iconos/tarjeta.svg';
import { ModalCarritoVencido } from './ModalCarritoVencido';
import type { CarritoDTO } from './types';
import { useCuentaRegresiva } from './useCuentaRegresiva';
import { nombreDeEvento, useEventos } from './useEventos';

export function CarritoPage() {
  const { usuario } = useAuth();
  const navigate = useNavigate();
  const [carrito, setCarrito] = useState<CarritoDTO | null>(null);
  const [recibidoEn, setRecibidoEn] = useState(() => Date.now());
  const [cargando, setCargando] = useState(true);
  const [errorDeCarga, setErrorDeCarga] = useState<unknown>(null);
  const [errorDeCompra, setErrorDeCompra] = useState<unknown>(null);
  const [procesando, setProcesando] = useState(false);
  const montado = useRef(true);

  const recibir = useCallback((nuevo: CarritoDTO) => {
    if (!montado.current) return;
    setCarrito(nuevo);
    setRecibidoEn(Date.now());
  }, []);

  useEffect(() => {
    montado.current = true;
    obtenerCarrito()
      .then(recibir)
      .catch((causa: unknown) => {
        if (montado.current) setErrorDeCarga(causa);
      })
      .finally(() => {
        if (montado.current) setCargando(false);
      });
    return () => {
      montado.current = false;
    };
  }, [recibir]);

  const cuenta = useCuentaRegresiva(carrito, recibidoEn);
  const eventos = useEventos(carrito?.items.map((item) => item.idEvento) ?? []);

  if (cargando) return <EstadoCarga mensaje="Cargando tu carrito..." />;
  if (errorDeCarga || !carrito) {
    return (
      <section className="stack-grande">
        <h2>Carrito</h2>
        <EstadoError error={errorDeCarga} />
      </section>
    );
  }

  if (carrito.items.length === 0) {
    return (
      <section className="stack-grande">
        <div className="encabezado-pagina">
          <div>
            <p className="eyebrow">Checkout</p>
            <h2>Tu carrito</h2>
          </div>
        </div>
        <div className="panel estado-vacio stack-chico">
          <h3>Tu carrito está vacío</h3>
          <p className="texto-secundario">Elegí entradas desde la cartelera para empezar una compra.</p>
          <div>
            <Link className="boton boton-primario" to="/">Ir a la cartelera</Link>
          </div>
        </div>
      </section>
    );
  }

  async function pagar() {
    setProcesando(true);
    setErrorDeCompra(null);
    try {
      const orden = await confirmarCompra();
      // La compra ya se confirmó: el DELETE solo reinicia el reloj del carrito para la próxima
      // compra de esta sesión (expiraEn no se reinicia al vaciar). Si falla, no hay nada que
      // mostrar — la orden existe igual.
      await vaciarCarrito().catch(() => undefined);
      navigate(`/mis-ordenes/${orden.id}`, { state: { recienConfirmada: true } });
    } catch (causa) {
      // 409 (cupo insuficiente, carrito vencido, bloqueo optimista), 402 (pago rechazado) o 422
      // (vacío). En todos los casos el backend NO vació el carrito: se vuelve a pedir para mostrar
      // el estado real y dejar reintentar sin perder la selección.
      setErrorDeCompra(causa);
      await obtenerCarrito().then(recibir).catch(() => undefined);
      if (montado.current) setProcesando(false);
    }
  }

  async function vaciar() {
    setProcesando(true);
    await vaciarCarrito().catch(() => undefined);
    navigate('/');
  }

  const idsEventos = [...new Set(carrito.items.map((item) => item.idEvento))];

  return (
    <section className="stack-grande">
      <div className="barra-hold" role="timer" aria-live="off">
        <img className="icono" src={iconoReloj} width={18} height={18} alt="" />
        <span>
          Tu carrito vence en <strong>{cuenta.texto}</strong> · el cupo se confirma al pagar
        </span>
      </div>

      <div className="checkout">
        <div className="checkout-columna">
          <ol className="pasos" aria-label="Pasos de la compra">
            <li className="paso paso-hecho">
              <span className="paso-numero">
                <img className="icono" src={iconoCheckPaso} width={14} height={14} alt="" />
              </span>
              Entradas
            </li>
            <li className="paso-separador" aria-hidden="true" />
            <li className="paso paso-actual" aria-current="step">
              <span className="paso-numero">2</span>
              Datos y pago
            </li>
            <li className="paso-separador" aria-hidden="true" />
            <li className="paso paso-pendiente">
              <span className="paso-numero">3</span>
              ¡Listo!
            </li>
          </ol>

          <h3>Tus datos</h3>
          <div className="fila-datos">
            <div className="dato-lectura">
              <span>Nombre</span>
              <span className="dato-lectura-valor">{usuario?.nombre}</span>
            </div>
            <div className="dato-lectura">
              <span>Email</span>
              <span className="dato-lectura-valor">{usuario?.email}</span>
            </div>
          </div>

          <h3>Medio de pago</h3>
          <div className="opcion-pago">
            <img className="icono" src={iconoRadio} width={18} height={18} alt="" />
            <img className="icono" src={iconoTarjeta} width={22} height={22} alt="" />
            <div>
              <p className="opcion-pago-titulo">Tarjeta de crédito / débito</p>
              <p className="opcion-pago-detalle">Pasarela simulada: el cobro se aprueba sin pedir datos de tarjeta</p>
            </div>
          </div>
          <p className="aviso-pasarela">
            <img className="icono" src={iconoCandado} width={14} height={14} alt="" />
            El pago se procesa con una pasarela externa (integración REST).
          </p>
        </div>

        <aside className="resumen-compra" aria-label="Resumen de la compra">
          {idsEventos.map((idEvento) => {
            const evento = eventos.get(idEvento);
            return (
              <div className="resumen-evento" key={idEvento}>
                <h3>{nombreDeEvento(eventos, idEvento)}</h3>
                {evento ? (
                  <p className="texto-secundario">
                    {formatearFechaEvento(evento.fechaHora)} · {evento.lugar}
                  </p>
                ) : null}
              </div>
            );
          })}
          <div className="resumen-divisor" />
          {carrito.items.map((item) => (
            <div className="resumen-linea" key={item.idTipoEntrada}>
              <span>
                {item.cantidad} × {item.nombreTipoEntrada}
                {idsEventos.length > 1 ? ` · ${nombreDeEvento(eventos, item.idEvento)}` : ''}
              </span>
              <span>{formatearPrecio(item.precioUnitario * item.cantidad)}</span>
            </div>
          ))}
          <div className="resumen-total">
            <span>Total</span>
            <span>{formatearPrecio(carrito.total)}</span>
          </div>
          {errorDeCompra ? <EstadoError error={errorDeCompra} /> : null}
          <button
            className="boton boton-primario boton-ancho boton-sombra"
            type="button"
            onClick={pagar}
            disabled={procesando || cuenta.vencido}
          >
            {procesando ? 'Procesando...' : errorDeCompra ? `Reintentar pago ${formatearPrecio(carrito.total)}` : `Pagar ${formatearPrecio(carrito.total)}`}
          </button>
          <button className="boton-texto" type="button" onClick={vaciar} disabled={procesando}>
            Vaciar carrito
          </button>
        </aside>
      </div>

      {cuenta.vencido ? <ModalCarritoVencido carrito={carrito} eventos={eventos} /> : null}
    </section>
  );
}
