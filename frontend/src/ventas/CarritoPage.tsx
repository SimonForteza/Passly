import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { EstadoCarga } from '../layout/EstadoCarga';
import { EstadoError } from '../layout/EstadoError';
import { ApiError } from '../lib/http/ApiError';
import { useAuth } from '../lib/auth/AuthContext';
import { verCarritoInvitado, transferirCarritoInvitado } from './invitadosApi';
import { confirmarCompra, obtenerCarrito, vaciarCarrito } from './api';
import { FormularioTarjeta } from './FormularioTarjeta';
import { formatearFechaEvento, formatearPrecio } from './formato';
import iconoCandado from './iconos/candado.svg';
import iconoCheckPaso from './iconos/check-paso.svg';
import iconoRadio from './iconos/radio-seleccionado.svg';
import iconoReloj from './iconos/reloj.svg';
import iconoTarjeta from './iconos/tarjeta.svg';
import { ModalCarritoVencido } from './ModalCarritoVencido';
import { digitosDeNumero, quitarPrefijoTarjeta, TARJETA_VACIA, validarTarjeta } from './tarjeta';
import type { CarritoDTO, DatosDeTarjetaDTO } from './types';
import { useCuentaRegresiva } from './useCuentaRegresiva';
import { nombreDeEvento, useEventos } from './useEventos';

function mensajeDePago(causa: unknown): string | null {
  if (!(causa instanceof ApiError)) return null;
  if (causa.status === 402) return 'El pago fue rechazado. Revisá los datos o probá con otra tarjeta.';
  if (causa.status === 503) return 'No pudimos confirmar el resultado del pago. No vuelvas a intentarlo hasta que verifiquemos qué pasó.';
  return null;
}

export function CarritoPage() {
  const { usuario } = useAuth();
  const navigate = useNavigate();
  const [carrito, setCarrito] = useState<CarritoDTO | null>(null);
  const [recibidoEn, setRecibidoEn] = useState(() => Date.now());
  const [cargando, setCargando] = useState(true);
  const [errorDeCarga, setErrorDeCarga] = useState<unknown>(null);
  const [errorDeCompra, setErrorDeCompra] = useState<unknown>(null);
  const [errorDePago, setErrorDePago] = useState<unknown>(null);
  const [pagoEnRevision, setPagoEnRevision] = useState(false);
  const [procesando, setProcesando] = useState(false);
  const [tarjeta, setTarjeta] = useState(TARJETA_VACIA);
  const [erroresTarjeta, setErroresTarjeta] = useState<Record<string, string>>({});
  const montado = useRef(true);

  const recibir = useCallback((nuevo: CarritoDTO) => {
    if (!montado.current) return;
    setCarrito(nuevo);
    setRecibidoEn(Date.now());
  }, []);

  useEffect(() => {
    montado.current = true;
    verCarritoInvitado()
      .then((invitado) =>
        invitado.items.length ? transferirCarritoInvitado() : obtenerCarrito(),
      )
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

  function cambiarTarjeta(nueva: DatosDeTarjetaDTO) {
    setTarjeta(nueva);
    if (Object.keys(erroresTarjeta).length > 0) {
      setErroresTarjeta(validarTarjeta(nueva));
    }
  }

  async function pagar() {
    const erroresDeValidacion = validarTarjeta(tarjeta);
    if (Object.keys(erroresDeValidacion).length > 0) {
      setErroresTarjeta(erroresDeValidacion);
      return;
    }

    setErroresTarjeta({});
    setProcesando(true);
    setErrorDeCompra(null);
    setErrorDePago(null);
    setPagoEnRevision(false);
    try {
      const orden = await confirmarCompra({
        ...tarjeta,
        numero: digitosDeNumero(tarjeta.numero),
      });
      setTarjeta(TARJETA_VACIA);
      await vaciarCarrito().catch(() => undefined);
      navigate(`/mis-ordenes/${orden.id}`, {
        replace: true,
        state: { recienConfirmada: true },
      });
    } catch (causa) {
      if (causa instanceof ApiError && causa.status === 400 && causa.errores) {
        setErroresTarjeta(quitarPrefijoTarjeta(causa.errores));
      } else if (causa instanceof ApiError && (causa.status === 402 || causa.status === 503)) {
        setErrorDePago(causa);
        setPagoEnRevision(causa.status === 503);
      } else {
        setErrorDeCompra(causa);
      }
      await obtenerCarrito().then(recibir).catch(() => undefined);
      if (montado.current) setProcesando(false);
    }
  }

  async function vaciar() {
    setProcesando(true);
    setErrorDeCompra(null);
    setErrorDePago(null);
    try {
      await vaciarCarrito();
      navigate('/');
    } catch (causa) {
      setErrorDeCompra(causa);
      setProcesando(false);
    }
  }

  if (cargando) return <EstadoCarga mensaje="Cargando tu carrito…" />;
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
              <p className="opcion-pago-detalle">
                Pasarela de prueba: <strong>4242 4242 4242 4242</strong> aprueba,{' '}
                <strong>4000 0000 0000 0002</strong> rechaza, <strong>4000 0000 0000 0119</strong>{' '}
                simula que la pasarela no responde.
              </p>
            </div>
          </div>

          <FormularioTarjeta
            tarjeta={tarjeta}
            onCambio={cambiarTarjeta}
            erroresCampo={erroresTarjeta}
            deshabilitado={procesando || pagoEnRevision}
          />

          <p className="aviso-pasarela">
            <img className="icono" src={iconoCandado} width={14} height={14} alt="" />
            El pago se procesa con la pasarela externa en modo de prueba.
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
          {errorDePago ? (
            mensajeDePago(errorDePago) ? (
              <p className="mensaje-error" role="alert">
                {mensajeDePago(errorDePago)}
              </p>
            ) : (
              <EstadoError error={errorDePago} />
            )
          ) : null}
          {errorDeCompra ? <EstadoError error={errorDeCompra} /> : null}
          <button
            className="boton boton-primario boton-ancho boton-sombra"
            type="button"
            onClick={pagar}
            disabled={procesando || cuenta.vencido || pagoEnRevision}
          >
            {procesando
              ? 'Procesando…'
              : pagoEnRevision
                ? 'Pago en revisión'
              : errorDePago
                ? `Reintentar pago ${formatearPrecio(carrito.total)}`
                : `Pagar ${formatearPrecio(carrito.total)}`}
          </button>
          <button className="boton-texto" type="button" onClick={vaciar} disabled={procesando || pagoEnRevision}>
            Vaciar carrito
          </button>
        </aside>
      </div>

      {cuenta.vencido && !pagoEnRevision ? (
        <ModalCarritoVencido carrito={carrito} eventos={eventos} />
      ) : null}
    </section>
  );
}
