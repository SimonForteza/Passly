import { formatearNumeroTarjeta, formatearVencimiento } from './tarjeta';
import type { DatosDeTarjetaDTO } from './types';

interface FormularioTarjetaProps {
  tarjeta: DatosDeTarjetaDTO;
  onCambio: (tarjeta: DatosDeTarjetaDTO) => void;
  erroresCampo: Record<string, string>;
  deshabilitado: boolean;
}

/** Presentacional: el estado de la tarjeta vive en la página de checkout y nunca se persiste. */
export function FormularioTarjeta({
  tarjeta,
  onCambio,
  erroresCampo,
  deshabilitado,
}: FormularioTarjetaProps) {
  function actualizar(campo: keyof DatosDeTarjetaDTO, valor: string) {
    onCambio({ ...tarjeta, [campo]: valor });
  }

  return (
    <div className="formulario-tarjeta">
      <label htmlFor="tarjeta-numero">Número de tarjeta</label>
      <input
        id="tarjeta-numero"
        inputMode="numeric"
        autoComplete="cc-number"
        placeholder="4242 4242 4242 4242"
        value={tarjeta.numero}
        onChange={(e) => actualizar('numero', formatearNumeroTarjeta(e.target.value))}
        disabled={deshabilitado}
        maxLength={23}
        aria-invalid={Boolean(erroresCampo.numero)}
        aria-describedby={erroresCampo.numero ? 'tarjeta-numero-error' : undefined}
      />
      {erroresCampo.numero ? (
        <span id="tarjeta-numero-error" className="ayuda-campo-error" role="alert">
          {erroresCampo.numero}
        </span>
      ) : null}

      <label htmlFor="tarjeta-titular">Titular</label>
      <input
        id="tarjeta-titular"
        autoComplete="cc-name"
        placeholder="Como figura en la tarjeta"
        value={tarjeta.titular}
        onChange={(e) => actualizar('titular', e.target.value)}
        disabled={deshabilitado}
        aria-invalid={Boolean(erroresCampo.titular)}
        aria-describedby={erroresCampo.titular ? 'tarjeta-titular-error' : undefined}
      />
      {erroresCampo.titular ? (
        <span id="tarjeta-titular-error" className="ayuda-campo-error" role="alert">
          {erroresCampo.titular}
        </span>
      ) : null}

      <div className="fila-datos">
        <div>
          <label htmlFor="tarjeta-vencimiento">Vencimiento</label>
          <input
            id="tarjeta-vencimiento"
            inputMode="numeric"
            autoComplete="cc-exp"
            placeholder="MM/AA"
            value={tarjeta.vencimiento}
            onChange={(e) => actualizar('vencimiento', formatearVencimiento(e.target.value))}
            disabled={deshabilitado}
            maxLength={5}
            aria-invalid={Boolean(erroresCampo.vencimiento)}
            aria-describedby={erroresCampo.vencimiento ? 'tarjeta-vencimiento-error' : undefined}
          />
          {erroresCampo.vencimiento ? (
            <span id="tarjeta-vencimiento-error" className="ayuda-campo-error" role="alert">
              {erroresCampo.vencimiento}
            </span>
          ) : null}
        </div>

        <div>
          <label htmlFor="tarjeta-codigo-seguridad">Código de seguridad</label>
          <input
            id="tarjeta-codigo-seguridad"
            type="password"
            inputMode="numeric"
            autoComplete="cc-csc"
            placeholder="CVV"
            value={tarjeta.codigoSeguridad}
            onChange={(e) => actualizar('codigoSeguridad', e.target.value.replace(/\D/g, '').slice(0, 4))}
            disabled={deshabilitado}
            maxLength={4}
            aria-invalid={Boolean(erroresCampo.codigoSeguridad)}
            aria-describedby={erroresCampo.codigoSeguridad ? 'tarjeta-codigo-seguridad-error' : undefined}
          />
          {erroresCampo.codigoSeguridad ? (
            <span id="tarjeta-codigo-seguridad-error" className="ayuda-campo-error" role="alert">
              {erroresCampo.codigoSeguridad}
            </span>
          ) : null}
        </div>
      </div>
    </div>
  );
}
