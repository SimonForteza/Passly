import { useEffect, useState, type FormEvent } from 'react';
import { EstadoError } from '../layout/EstadoError';
import { httpClient } from '../lib/http/httpClient';
import { listarMiembros } from './api';
import { useProductoraActual } from './GestionLayout';
import { InvitacionesEquipo } from './InvitacionesEquipo';
import type { MiembroDTO } from './types';
export function BackofficePage() {
  const p = useProductoraActual();
  const [miembros, setMiembros] = useState<MiembroDTO[]>([]);
  const [email, setEmail] = useState('');
  const [rol, setRol] = useState('STAFF');
  const [revision, setRevision] = useState(0);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [mensaje, setMensaje] = useState('');
  useEffect(() => {
    let vigente = true;
    listarMiembros(p.id)
      .then((v) => {
        if (vigente) setMiembros(v);
      })
      .catch((e) => {
        if (vigente) setError(e);
      });
    return () => {
      vigente = false;
    };
  }, [p.id, revision]);
  async function invitar(e: FormEvent) {
    e.preventDefault();
    setGuardando(true);
    setError(null);
    setMensaje('');
    try {
      const destinatario = email.trim().toLowerCase();
      await httpClient.post('/api/productoras/' + p.id + '/invitaciones', {
        email: destinatario,
        rolEnProductora: rol,
      });
      setMensaje(
        'Invitación creada para ' +
          destinatario +
          '. El correo está en la cola de envío.',
      );
      setEmail('');
      setRevision((v) => v + 1);
    } catch (causa) {
      setError(causa);
    } finally {
      setGuardando(false);
    }
  }
  return (
    <section className="stack-grande">
      <div>
        <p className="eyebrow">{p.nombreComercial}</p>
        <h2>Equipo</h2>
        <p className="texto-secundario">
          Tu rol en esta productora:{' '}
          {p.rol === 'DUENIO'
            ? 'dueño'
            : p.rol === 'STAFF'
              ? 'staff'
              : 'validador'}
          .
        </p>
      </div>
      {error ? <EstadoError error={error} /> : null}
      {mensaje ? (
        <p className="mensaje-exito" role="status">
          {mensaje}
        </p>
      ) : null}
      {p.puedeInvitar ? (
        <form className="panel formulario" onSubmit={invitar}>
          <h3>Invitar al equipo</h3>
          <p>
            La persona puede tener cuenta o registrarse al recibir la
            invitación. El enlace dura siete días.
          </p>
          <label>
            Email de la persona
            <input
              type="email"
              required
              maxLength={254}
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </label>
          <label>
            Tarea dentro de la productora
            <select value={rol} onChange={(e) => setRol(e.target.value)}>
              <option value="STAFF">Staff — gestión de eventos</option>
              <option value="VALIDADOR">Validador — control de ingreso</option>
            </select>
          </label>
          <p className="ayuda-campo">
            {rol === 'STAFF'
              ? 'Puede crear, editar y publicar eventos de esta productora. Solo el dueño administra las invitaciones.'
              : 'Puede controlar los QR de los eventos publicados de esta productora. No puede gestionar eventos ni invitar personas.'}
          </p>
          <button className="boton boton-primario" disabled={guardando}>
            {guardando ? 'Creando invitación…' : 'Enviar invitación'}
          </button>
        </form>
      ) : null}
      <div className="panel">
        <div className="titulo-seccion">
          <h3>Integrantes</h3>
          <span className="contador">
            {miembros.length} {miembros.length === 1 ? 'persona' : 'personas'}
          </span>
        </div>
        <div className="tabla-responsive">
          <table className="tabla-equipo">
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Email</th>
                <th>Tarea</th>
              </tr>
            </thead>
            <tbody>
              {miembros.map((m) => (
                <tr key={m.idUsuario}>
                  <td data-label="Nombre">{m.nombre}</td>
                  <td data-label="Email">{m.email}</td>
                  <td data-label="Tarea">
                    {m.rolEnProductora === 'DUENIO'
                      ? 'Dueño'
                      : m.rolEnProductora === 'STAFF'
                        ? 'Staff'
                        : 'Validador'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
      {p.puedeInvitar ? (
        <InvitacionesEquipo id={p.id} revision={revision} />
      ) : null}
    </section>
  );
}
