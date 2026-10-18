import { useCallback, useEffect, useRef, useState } from 'react';
import { useAuth } from '../lib/auth/AuthContext';
import { misProductoras } from '../productoras/api';
import { listarEventos } from '../eventos/api';
import type { EventoDTO } from '../eventos/types';
import { EstadoError } from '../layout/EstadoError';
import { CamaraQr } from './CamaraQr';
import { validarEntrada, type ValidacionDTO } from './api';
import './accesos.css';

export function ValidarPage() {
  const { usuario } = useAuth();
  const permitido = usuario?.rol === 'VALIDADOR' || usuario?.rol === 'ORGANIZADOR';
  const [eventos, setEventos] = useState<EventoDTO[]>([]);
  const [idEvento, setIdEvento] = useState('');
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<unknown>(null);
  const [contenido, setContenido] = useState('');
  const [camara, setCamara] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [resultado, setResultado] = useState<ValidacionDTO | null>(null);
  const ocupado = useRef(false);
  const vigente = useRef(true);

  useEffect(() => {
    vigente.current = true;
    return () => { vigente.current = false; };
  }, []);

  useEffect(() => {
    if (!permitido) return;
    let actual = true;
    misProductoras().then(async (productoras) => {
      const grupos = await Promise.all(productoras.map((p) => listarEventos(p.id)));
      if (actual) setEventos(grupos.flat().sort((a, b) => a.fechaHora.localeCompare(b.fechaHora)));
    }).catch((causa: unknown) => { if (actual) setError(causa); })
      .finally(() => { if (actual) setCargando(false); });
    return () => { actual = false; };
  }, [permitido]);

  const reiniciar = useCallback(() => {
    ocupado.current = false;
    setResultado(null);
    setContenido('');
  }, []);

  useEffect(() => {
    if (!resultado) return;
    const timer = window.setTimeout(reiniciar, 3000);
    return () => window.clearTimeout(timer);
  }, [resultado, reiniciar]);

  const validar = useCallback((texto: string) => {
    if (ocupado.current || !idEvento || !texto.trim()) return;
    ocupado.current = true;
    setEnviando(true);
    setError(null);
    validarEntrada(Number(idEvento), texto.trim()).then((respuesta) => {
      if (vigente.current) setResultado(respuesta);
    }).catch((causa: unknown) => {
      if (vigente.current) {
        setError(causa);
        setCamara(false);
        ocupado.current = false;
      }
    }).finally(() => { if (vigente.current) setEnviando(false); });
  }, [idEvento]);

  if (!permitido) return <section className="stack-grande"><h1>Validar entradas</h1>
    <p>Tu cuenta no tiene permiso para validar entradas.</p></section>;

  const evento = eventos.find((e) => String(e.id) === idEvento);
  return <section className="validador">
    <header className="validador-titulo"><span>Control de acceso</span><h1>Validar entradas</h1>
      <p>Elegí el evento y escaneá cada entrada en la puerta.</p></header>
    <label className="validador-evento">Evento
      <select value={idEvento} disabled={cargando || enviando || !!resultado} onChange={(e) => {
        setIdEvento(e.target.value); setCamara(false); setError(null); reiniciar();
      }}>
        <option value="">{cargando ? 'Cargando eventos…' : 'Elegí un evento'}</option>
        {eventos.map((e) => <option key={e.id} value={e.id}>{e.nombre} · {e.organizador.nombreComercial}</option>)}
      </select>
    </label>
    {!cargando && eventos.length === 0 && !error ? <p>No hay eventos publicados de tus productoras para validar.</p> : null}
    {error ? <EstadoError error={error} /> : null}
    {idEvento ? <>
      <div className="validador-puesto"><span>Puerta de acceso</span><strong>{evento?.nombre}</strong></div>
      {camara && !enviando && !resultado ? <CamaraQr onLeer={validar} /> : null}
      <button className="validador-boton" type="button" disabled={enviando || !!resultado}
        onClick={() => setCamara(!camara)}>{camara ? 'Apagar cámara' : 'Activar cámara'}</button>
      <form className="validador-manual" onSubmit={(e) => { e.preventDefault(); validar(contenido); }}>
        <label htmlFor="contenido-qr">Ingreso manual del QR</label>
        <p>Si no podés usar la cámara, pegá el contenido de la entrada.</p>
        <textarea id="contenido-qr" rows={3} maxLength={512} value={contenido} disabled={enviando || !!resultado}
          onChange={(e) => setContenido(e.target.value)} placeholder="PASSLY:v1:…" autoCapitalize="off" spellCheck={false} />
        <button className="validador-boton validador-boton-principal" disabled={!contenido.trim() || enviando || !!resultado}>
          {enviando ? 'Validando entrada…' : 'Validar entrada'}</button>
      </form>
    </> : null}
    {enviando ? <p role="status">Verificando la entrada…</p> : null}
    {resultado ? <div className={`validador-resultado resultado-${resultado.resultado.toLowerCase()}`} role="status" aria-live="assertive">
      <span className="validador-simbolo" aria-hidden="true">{resultado.resultado === 'VALIDO' ? '✓' : resultado.resultado === 'YA_USADO' ? '!' : '×'}</span>
      <h2>{({ VALIDO: 'Válido', YA_USADO: 'Ya usado', OTRO_EVENTO: 'Otro evento', INVALIDO: 'Inválido' })[resultado.resultado]}</h2>
      <p>{resultado.mensaje}</p>
      {resultado.ticket ? <strong>{resultado.ticket.nombreTipoEntrada}</strong> : null}
      {resultado.resultado === 'YA_USADO' && resultado.ticket ? <p>Primer uso: {new Date(resultado.ticket.usadoEn).toLocaleString('es-AR')}</p> : null}
      <button type="button" className="validador-boton" onClick={reiniciar}>Validar siguiente entrada</button>
      <small>El escaneo continúa en 3 segundos.</small>
    </div> : null}
  </section>;
}
