import { avisarSesionVencida, getToken } from '../auth/session';
import { ApiError, MENSAJE_SIN_CONEXION, type ProblemDetail } from './ApiError';

const BASE_URL = import.meta.env.DEV
  ? ''
  : ((import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '');

// Cola de los requests autenticados. Spring Security le cambia el id a la sesion en CADA request
// autenticado (ChangeSessionIdAuthenticationStrategy, CLAUDE.md 4.7) y Tomcat descarta el id
// viejo en el acto. Pasar de Basic a JWT no lo cambio: verificado en PAS-33, con Bearer el
// JSESSIONID sigue rotando, asi que la cola se queda. Si dos requests autenticados salen en paralelo con la misma cookie, el que
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

export interface OpcionesRequest extends RequestInit {
  /**
   * No manda el token guardado. Lo usa el login: el backend responde 401 a cualquier token
   * invalido aunque la ruta sea publica, y un token viejo en sessionStorage no tiene que poder
   * impedir que alguien vuelva a iniciar sesion.
   */
  anonimo?: boolean;
  /** 'blob' para bajar un archivo (la portada de un borrador, que pide token). */
  formato?: 'json' | 'blob';
}

/**
 * URL absoluta de un recurso de la API, para usar fuera de fetch (un <img src>). En desarrollo
 * queda relativa y la resuelve el proxy de Vite.
 */
export function urlDeApi(path: string): string {
  return `${BASE_URL}${path}`;
}

function request<T>(path: string, opciones: OpcionesRequest = {}): Promise<T> {
  const { anonimo, formato, ...init } = opciones;
  const headers = new Headers(init.headers);
  headers.set('Accept', formato === 'blob' ? '*/*' : 'application/json');
  // Con FormData (subida de archivos) el Content-Type lo arma el navegador, con el boundary del
  // multipart. Fijarlo a mano rompería la subida.
  if (init.body && !(init.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  const token = anonimo ? null : getToken();
  if (token) headers.set('Authorization', `Bearer ${token}`);

  const enviar = () => enviarRequest<T>(path, { ...init, headers }, token, formato);
  return token ? encolar(enviar) : enviar();
}

async function enviarRequest<T>(
  path: string,
  init: RequestInit & { headers: Headers },
  token: string | null,
  formato: 'json' | 'blob' = 'json',
): Promise<T> {
  const { headers } = init;
  let respuesta: Response;
  try {
    respuesta = await fetch(`${BASE_URL}${path}`, {
      ...init,
      headers,
      // El carrito de ventas vive en una HttpSession (@SessionScope) aunque la
      // autenticación sea STATELESS: sin esto la cookie JSESSIONID no viaja y el
      // carrito no persiste entre llamadas (CLAUDE.md §4.7 / §4.11).
      credentials: 'include',
    });
  } catch (causa) {
    // fetch solo rechaza cuando no hubo respuesta (red caida, backend apagado sin proxy, CORS).
    // Un abort es decision de quien llamo: se propaga tal cual.
    if (causa instanceof DOMException && causa.name === 'AbortError') throw causa;
    throw new ApiError(
      {
        status: 0,
        title: 'Sin conexión',
        detail: MENSAJE_SIN_CONEXION,
        type: 'about:blank',
        instance: path,
      },
      true,
    );
  }

  if (respuesta.status === 204) {
    return undefined as T;
  }

  if (formato === 'blob' && respuesta.ok) {
    return (await respuesta.blob()) as T;
  }

  const contentType = respuesta.headers.get('content-type') ?? '';
  const cuerpo = contentType.includes('json')
    ? await respuesta.json()
    : undefined;

  if (!respuesta.ok) {
    // 401 con token = el backend rechazo el token (vencido, adulterado o de una cuenta que ya no
    // existe). Se cierra la sesion y App.tsx manda al login con "from". Solo si el token rechazado
    // es el que sigue guardado: si en el medio alguien inicio sesion de nuevo, no se la borramos.
    if (respuesta.status === 401 && token && getToken() === token) {
      avisarSesionVencida();
    }
    if (cuerpo) throw new ApiError(cuerpo as ProblemDetail);
    // Sin ProblemDetail la respuesta no la armo el backend: un 502/503/504 es el proxy (Vite en
    // desarrollo) avisando que el backend no contesta. No se usa statusText: llega en ingles
    // ("Bad Gateway") y no le dice nada a quien usa la app.
    const sinConexion = [502, 503, 504].includes(respuesta.status);
    throw new ApiError(
      {
        status: respuesta.status,
        title: sinConexion ? 'Sin conexión' : 'Error',
        detail: sinConexion ? MENSAJE_SIN_CONEXION : '',
        type: 'about:blank',
        instance: path,
      },
      sinConexion,
    );
  }

  return cuerpo as T;
}

export const httpClient = {
  get<T>(path: string, init: OpcionesRequest = {}): Promise<T> {
    return request<T>(path, { ...init, method: 'GET' });
  },
  post<T>(path: string, body?: unknown, init: OpcionesRequest = {}): Promise<T> {
    return request<T>(path, {
      ...init,
      method: 'POST',
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  },
  put<T>(path: string, body?: unknown): Promise<T> {
    return request<T>(path, {
      method: 'PUT',
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  },
  delete<T>(path: string): Promise<T> {
    return request<T>(path, { method: 'DELETE' });
  },
  /** PUT multipart: el archivo viaja como FormData. */
  putArchivo<T>(path: string, datos: FormData): Promise<T> {
    return request<T>(path, { method: 'PUT', body: datos });
  },
  blob(path: string): Promise<Blob> {
    return request<Blob>(path, { method: 'GET', formato: 'blob' });
  },
};
