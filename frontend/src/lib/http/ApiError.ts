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

export class ApiError extends Error {
  readonly status: number;
  readonly title: string;
  readonly detail: string;
  readonly instance: string;
  readonly errores?: Record<string, string>;

  constructor(problema: ProblemDetail) {
    super(problema.detail || problema.title);
    this.name = 'ApiError';
    this.status = problema.status;
    this.title = problema.title;
    this.detail = problema.detail;
    this.instance = problema.instance;
    this.errores = problema.errores;
  }
}
