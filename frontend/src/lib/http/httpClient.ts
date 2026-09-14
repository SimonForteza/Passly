import { getHeaderAutorizacion } from '../auth/session';
import { ApiError, type ProblemDetail } from './ApiError';

const BASE_URL = import.meta.env.VITE_API_BASE_URL as string;

// Cola de los requests autenticados. Spring Security le cambia el id a la sesion en CADA request
// autenticado (ChangeSessionIdAuthenticationStrategy, CLAUDE.md 4.7) y Tomcat descarta el id
// viejo en el acto. Si dos requests autenticados salen en paralelo con la misma cookie, el que
// llega segundo trae un JSESSIONID que ya no existe: si ese request toca el carrito
// (@SessionScope), el contenedor le crea una sesion nueva y el carrito aparece vacio. Encolarlos
// garantiza que cada uno salga con la cookie que dejo el anterior. Los anonimos no rotan nada y
// siguen en paralelo.
let colaAutenticada: Promise<unknown> = Promise.resolve();

function encolar<T>(tarea: () => Promise<T>): Promise<T> {
  const resultado = colaAutenticada.then(tarea, tarea);
  colaAutenticada = resultado.catch(() => undefined);
  return resultado;
}

function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set('Accept', 'application/json');
  if (init.body) headers.set('Content-Type', 'application/json');

  // El header explicito gana: el login valida credenciales pegandole a /api/usuarios/me con un
  // Basic armado a mano (todavia no hay sesion guardada), y no queremos que la sesion actual —
  // si la hubiera — lo pise.
  if (!headers.has('Authorization')) {
    const autorizacion = getHeaderAutorizacion();
    if (autorizacion) headers.set('Authorization', autorizacion);
  }

  const enviar = () => enviarRequest<T>(path, { ...init, headers });
  return headers.has('Authorization') ? encolar(enviar) : enviar();
}

async function enviarRequest<T>(path: string, init: RequestInit & { headers: Headers }): Promise<T> {
  const { headers } = init;
  const respuesta = await fetch(`${BASE_URL}${path}`, {
    ...init,
    headers,
    // El carrito de ventas vive en una HttpSession (@SessionScope) aunque la
    // autenticación sea STATELESS: sin esto la cookie JSESSIONID no viaja y el
    // carrito no persiste entre llamadas (CLAUDE.md §4.7 / §4.11).
    credentials: 'include',
  });

  if (respuesta.status === 204) {
    return undefined as T;
  }

  const contentType = respuesta.headers.get('content-type') ?? '';
  const cuerpo = contentType.includes('json') ? await respuesta.json() : undefined;

  if (!respuesta.ok) {
    throw new ApiError(cuerpo as ProblemDetail);
  }

  return cuerpo as T;
}

export const httpClient = {
  get<T>(path: string, init: RequestInit = {}): Promise<T> {
    return request<T>(path, { ...init, method: 'GET' });
  },
  post<T>(path: string, body?: unknown): Promise<T> {
    return request<T>(path, { method: 'POST', body: body !== undefined ? JSON.stringify(body) : undefined });
  },
  delete<T>(path: string): Promise<T> {
    return request<T>(path, { method: 'DELETE' });
  },
};
