/**
 * Cliente de la API de Passly para el validador.
 *
 * Usa exactamente los mismos endpoints que la pantalla web /validar: la API no cambia entre
 * la web y la app (CLAUDE.md §2). El token vive solo en memoria de la app.
 */

export type Resultado = 'VALIDO' | 'YA_USADO' | 'OTRO_EVENTO' | 'INVALIDO';

export interface ValidacionDTO {
  resultado: Resultado;
  mensaje: string;
  ticket: { id: number; idEvento: number; nombreTipoEntrada: string; usadoEn: string | null } | null;
}

export interface EventoDTO {
  id: number;
  nombre: string;
  lugar: string;
  fechaHora: string;
  estado: string;
}

interface ProductoraDTO {
  id: number;
}

export interface Sesion {
  servidor: string;
  token: string;
  nombre: string;
}

/** 401 con token: la sesión venció o el token no sirve. La app vuelve al login. */
export class SesionVencidaError extends Error {}

const TIMEOUT_MS = 8000;

async function pedir<T>(servidor: string, ruta: string, opciones: RequestInit = {}, token?: string): Promise<T> {
  const controlador = new AbortController();
  const temporizador = setTimeout(() => controlador.abort(), TIMEOUT_MS);
  let respuesta: Response;
  try {
    respuesta = await fetch(servidor.replace(/\/+$/, '') + ruta, {
      ...opciones,
      signal: controlador.signal,
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    });
  } catch {
    throw new Error('No llego al servidor. Revisá la dirección, que el backend esté corriendo y que el celular esté en la misma red.');
  } finally {
    clearTimeout(temporizador);
  }

  if (respuesta.status === 401 && token) throw new SesionVencidaError('La sesión venció. Ingresá de nuevo.');
  if (!respuesta.ok) {
    let detalle = '';
    try {
      const problema = await respuesta.json();
      detalle = problema.detail ?? problema.title ?? '';
    } catch {
      // cuerpo vacío o no JSON
    }
    throw new Error(detalle || `El servidor respondió ${respuesta.status}.`);
  }
  return (await respuesta.json()) as T;
}

export async function iniciarSesion(servidor: string, email: string, password: string): Promise<Sesion> {
  const r = await pedir<{ token: string; usuario: { nombre: string } }>(servidor, '/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
  return { servidor, token: r.token, nombre: r.usuario.nombre };
}

/** Eventos publicados de las productoras de quien valida, igual que la web /validar. */
export async function listarEventosParaValidar(sesion: Sesion): Promise<EventoDTO[]> {
  const productoras = await pedir<ProductoraDTO[]>(sesion.servidor, '/api/productoras/mias', {}, sesion.token);
  const grupos = await Promise.all(
    productoras.map((p) =>
      pedir<EventoDTO[]>(sesion.servidor, `/api/eventos?productora=${p.id}`, {}, sesion.token),
    ),
  );
  return grupos
    .flat()
    .filter((e) => e.estado === 'PUBLICADO')
    .sort((a, b) => a.fechaHora.localeCompare(b.fechaHora));
}

export function validarEntrada(sesion: Sesion, idEvento: number, contenidoQr: string): Promise<ValidacionDTO> {
  return pedir<ValidacionDTO>(
    sesion.servidor,
    '/api/accesos/validaciones',
    { method: 'POST', body: JSON.stringify({ idEvento, contenidoQr }) },
    sesion.token,
  );
}
