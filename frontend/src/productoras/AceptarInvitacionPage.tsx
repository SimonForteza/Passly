import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { httpClient } from '../lib/http/httpClient';
import { useAuth } from '../lib/auth/AuthContext';
import { EstadoError } from '../layout/EstadoError';
import { useOrganizaciones } from './OrganizacionContext';
interface Detalle {
  idProductora: number;
  email: string;
  productora: string;
  rol: string;
  estado: string;
  venceEn: string;
}
export function AceptarInvitacionPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { estaAutenticado, actualizarUsuario, usuario, cerrarSesion } =
    useAuth();
  const { recargar } = useOrganizaciones();
  const token = location.hash.slice(1);
  const [detalle, setDetalle] = useState<Detalle | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [ocupado, setOcupado] = useState(false);
  useEffect(() => {
    let vigente = true;
    if (!token) return;
    httpClient
      .get<Detalle>('/api/invitaciones', {
        headers: { 'X-Invitacion-Token': token },
      })
      .then((v) => {
        if (vigente) setDetalle(v);
      })
      .catch((e) => {
        if (vigente) setError(e);
      });
    return () => {
      vigente = false;
    };
  }, [token]);
  const destino = (id: number, rol: string) =>
    rol === 'VALIDADOR'
      ? '/validar?productora=' + id
      : '/productoras/' + id + '/eventos';
  async function aceptar() {
    setOcupado(true);
    setError(null);
    try {
      const id = await httpClient.post<number>(
        '/api/invitaciones/aceptacion',
        undefined,
        { headers: { 'X-Invitacion-Token': token } },
      );
      await actualizarUsuario();
      await recargar();
      navigate(destino(id, detalle!.rol), {
        replace: true,
        state: { invitacionAceptada: true },
      });
    } catch (e) {
      setError(e);
    } finally {
      setOcupado(false);
    }
  }
  const coincide =
    usuario?.email.toLowerCase() === detalle?.email.toLowerCase();
  return (
    <section className="panel formulario">
      <p className="eyebrow">Tu equipo en Passly</p>
      <h2>Invitación a una productora</h2>
      {error ? <EstadoError error={error} /> : null}
      {!token ? (
        <p>
          Este enlace está incompleto. Abrí el enlace del correo de invitación.
        </p>
      ) : !detalle && !error ? (
        <p>Cargando invitación…</p>
      ) : null}
      {detalle ? (
        <>
          <p>
            <strong>{detalle.productora}</strong> te invita como{' '}
            {detalle.rol === 'STAFF' ? 'staff' : 'validador'}.
          </p>
          <p>
            {detalle.rol === 'STAFF'
              ? 'Vas a poder crear y gestionar los eventos de esta productora.'
              : 'Vas a poder controlar el ingreso con los QR de los eventos de esta productora.'}
          </p>
          <p className="texto-secundario">
            Invitación para {detalle.email}. Vence el{' '}
            {new Date(detalle.venceEn).toLocaleDateString('es-AR')}.
          </p>
          {!['PENDIENTE', 'ACEPTADA'].includes(detalle.estado) ? (
            <p>
              Esta invitación{' '}
              {detalle.estado === 'REVOCADA' ? 'fue revocada' : 'venció'}.
              Pedile al dueño que te envíe una nueva.
            </p>
          ) : estaAutenticado ? (
            !coincide ? (
              <>
                <p className="aviso-informativo">
                  Ingresaste como {usuario?.email}. Para usar esta invitación,
                  ingresá con {detalle.email}.
                </p>
                <button
                  className="boton boton-secundario"
                  onClick={cerrarSesion}
                >
                  Usar otra cuenta
                </button>
              </>
            ) : detalle.estado === 'ACEPTADA' ? (
              <>
                <p className="mensaje-exito">Ya formás parte de este equipo.</p>
                <Link
                  className="boton boton-primario"
                  to={destino(detalle.idProductora, detalle.rol)}
                >
                  Ir a mi tarea
                </Link>
              </>
            ) : (
              <>
                <p>Ingresaste con el email invitado: {usuario?.email}.</p>
                <button
                  className="boton boton-primario"
                  disabled={ocupado}
                  onClick={aceptar}
                >
                  {ocupado ? 'Aceptando…' : 'Aceptar invitación'}
                </button>
              </>
            )
          ) : (
            <div className="acciones">
              <Link
                className="boton boton-primario"
                to="/login"
                state={{
                  from: location,
                  contexto: 'Ingresá para sumarte a ' + detalle.productora,
                }}
              >
                Ingresar con mi cuenta
              </Link>
              <Link
                className="boton boton-secundario"
                to="/registro"
                state={{
                  from: location,
                  contexto:
                    'Creá tu cuenta para sumarte a ' + detalle.productora,
                }}
              >
                Crear cuenta
              </Link>
            </div>
          )}
        </>
      ) : null}
    </section>
  );
}
