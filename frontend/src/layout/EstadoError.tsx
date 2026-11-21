import { esSinConexion, mensajeDeError } from '../lib/http/ApiError';

export function EstadoError({
  error,
  onReintentar,
}: {
  error: unknown;
  /** Si viene, se ofrece un botón para volver a intentar la carga. */
  onReintentar?: () => void;
}) {
  const sinConexion = esSinConexion(error);

  return (
    <div role="alert" className="estado-error">
      {sinConexion ? <strong className="estado-error-titulo">Sin conexión</strong> : null}
      <p>{mensajeDeError(error)}</p>
      {onReintentar ? (
        <button type="button" className="boton boton-secundario" onClick={onReintentar}>
          Reintentar
        </button>
      ) : null}
    </div>
  );
}
