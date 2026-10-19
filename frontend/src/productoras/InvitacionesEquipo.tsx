import { useEffect, useState } from 'react';
import { httpClient } from '../lib/http/httpClient';
import { EstadoError } from '../layout/EstadoError';
interface Invitacion {
  id: number;
  email: string;
  rol: string;
  estado: string;
  venceEn: string;
  entrega: string;
}
const ESTADOS: Record<string, string> = {
  PENDIENTE: 'Pendiente de aceptación',
  ACEPTADA: 'Aceptada',
  REVOCADA: 'Revocada',
  VENCIDA: 'Vencida',
};
const ENTREGA: Record<string, string> = {
  PENDIENTE: 'Correo en cola',
  ENVIADO: 'Correo enviado',
  REINTENTO: 'Reintentando envío',
  ERROR: 'Reintentando envío',
  FALLIDO: 'Reintentando envío',
};
export function InvitacionesEquipo({
  id,
  revision,
}: {
  id: number;
  revision: number;
}) {
  const [filas, setFilas] = useState<Invitacion[]>([]);
  const [error, setError] = useState<unknown>(null);
  const [ocupado, setOcupado] = useState<number | null>(null);
  const [cambio, setCambio] = useState(0);
  const [mensaje, setMensaje] = useState('');
  useEffect(() => {
    let vigente = true;
    const cargar = () =>
      httpClient
        .get<Invitacion[]>('/api/productoras/' + id + '/invitaciones')
        .then((v) => {
          if (vigente) {
            setFilas(v);
            setError(null);
          }
        })
        .catch((e) => {
          if (vigente) setError(e);
        });
    void cargar();
    const timer = window.setInterval(() => {
      void cargar();
    }, 15000);
    return () => {
      vigente = false;
      window.clearInterval(timer);
    };
  }, [id, revision, cambio]);
  async function actuar(i: Invitacion, reenviar: boolean) {
    if (
      !reenviar &&
      !window.confirm(
        '¿Revocar la invitación de ' +
          i.email +
          '? Su enlace dejará de funcionar.',
      )
    )
      return;
    setOcupado(i.id);
    setError(null);
    setMensaje('');
    try {
      const ruta = '/api/productoras/' + id + '/invitaciones/' + i.id;
      if (reenviar) await httpClient.post(ruta + '/reenvio');
      else await httpClient.delete(ruta);
      setMensaje(
        reenviar
          ? 'Se generó un nuevo enlace para ' +
              i.email +
              '. El anterior dejó de funcionar; el nuevo correo está en cola.'
          : 'Invitación revocada para ' + i.email + '.',
      );
      setCambio((v) => v + 1);
    } catch (e) {
      setError(e);
    } finally {
      setOcupado(null);
    }
  }
  return (
    <section className="panel stack-chico">
      <h3>Invitaciones</h3>
      {error ? <EstadoError error={error} /> : null}
      {mensaje ? (
        <p className="mensaje-exito" role="status">
          {mensaje}
        </p>
      ) : null}
      {!filas.length ? (
        <p>No hay invitaciones enviadas.</p>
      ) : (
        <ul className="invitaciones-lista">
          {filas.map((i) => (
            <li key={i.id}>
              <div>
                <strong className="email-equipo">{i.email}</strong>
                <p>
                  {i.rol === 'STAFF' ? 'Staff' : 'Validador'} ·{' '}
                  {ESTADOS[i.estado] ?? 'Estado no disponible'}
                </p>
                <p className="texto-secundario">
                  {ENTREGA[i.entrega] ?? 'Consultando entrega'} ·{' '}
                  {i.estado === 'PENDIENTE' ? 'Vence el ' : 'Vencimiento: '}
                  {new Date(i.venceEn).toLocaleDateString('es-AR')}
                </p>
              </div>
              {['PENDIENTE', 'VENCIDA'].includes(i.estado) ? (
                <div className="acciones">
                  <button
                    className="boton boton-secundario"
                    disabled={ocupado !== null}
                    onClick={() => actuar(i, true)}
                  >
                    {ocupado === i.id ? 'Actualizando…' : 'Reenviar enlace'}
                  </button>
                  <button
                    className="boton boton-secundario"
                    disabled={ocupado !== null}
                    onClick={() => actuar(i, false)}
                  >
                    Revocar invitación
                  </button>
                </div>
              ) : null}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
