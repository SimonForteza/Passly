// Forma de ProblemDetail (RFC 9457) que devuelven los @RestControllerAdvice del backend.
// "errores" es la única extensión propia: un mapa campo -> mensaje en los 400 de validación.
export interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance: string;
  errores?: Record<string, string>;
}

export const MENSAJE_SIN_CONEXION =
  'No pudimos conectarnos con Passly. Revisá tu conexión e intentá de nuevo en unos segundos.';

export class ApiError extends Error {
  readonly status: number;
  readonly title: string;
  readonly detail: string;
  readonly instance: string;
  readonly errores?: Record<string, string>;
  /**
   * El backend no respondió: la red falló o un proxy contestó 502/503/504 sin ProblemDetail.
   * Distinto de un error de negocio, que siempre trae su ProblemDetail.
   */
  readonly sinConexion: boolean;

  constructor(problema: ProblemDetail, sinConexion = false) {
    super(problema.detail || problema.title);
    this.name = 'ApiError';
    this.status = problema.status;
    this.title = problema.title;
    this.detail = problema.detail;
    this.instance = problema.instance;
    this.errores = problema.errores;
    this.sinConexion = sinConexion;
  }
}

export function esSinConexion(error: unknown): boolean {
  return (error instanceof ApiError && error.sinConexion) || error instanceof TypeError;
}

/**
 * Único lugar que traduce un error a texto para la persona. Nunca muestra el statusText crudo
 * ("Bad Gateway"): si el backend no mandó un detail propio, se usa un mensaje en castellano.
 */
export function mensajeDeError(error: unknown): string {
  if (esSinConexion(error)) return MENSAJE_SIN_CONEXION;
  if (!(error instanceof ApiError)) {
    return 'No pudimos completar la acción. Intentá de nuevo.';
  }
  if (error.status === 401) {
    return 'Tu sesión no está disponible. Ingresá nuevamente para continuar.';
  }
  if (error.status === 403 && !error.detail?.includes('origen de la compra')) {
    return 'Tu cuenta no tiene acceso a esta acción. Revisá la cuenta y la productora seleccionadas.';
  }
  if (error.detail) return error.detail;
  if (error.status >= 500) {
    return 'Tuvimos un problema de nuestro lado. Intentá de nuevo en unos minutos.';
  }
  return 'No pudimos completar la acción. Intentá de nuevo.';
}
