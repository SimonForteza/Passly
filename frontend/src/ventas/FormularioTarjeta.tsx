import { formatearNumeroTarjeta, formatearVencimiento } from './tarjeta';
import type { DatosDeTarjetaDTO } from './types';

interface FormularioTarjetaProps {
  tarjeta: DatosDeTarjetaDTO;
  onCambio: (tarjeta: DatosDeTarjetaDTO) => void;
  erroresCampo: Record<string, string>;
  deshabilitado: boolean;
}

/**
 * Puramente presentacional: no guarda estado propio, para que CarritoPage sea la unica fuente
 * de verdad del dato (y quien decide cuando se limpia). No hay autocomplete propio del
 * navegador mas alla de `autoComplete`: sin libreria de mascaras, el formateo es manual
 * (ver tarjeta.ts).
 */
export function FormularioTarjeta({ tarjeta, onCambio, erroresCampo, deshabilitado }: FormularioTarjetaProps) {
  function actualizar(campo: keyof DatosDeTarjetaDTO, valor: string) {
    onCambio({ ...tarjeta, [campo]: valor });
  }

  return (
    <div className="formulario-tarjeta">
      <label>
        Numero de tarjeta
        <input
          inputMode="numeric"
          autoComplete="cc-number"
          placeholder="4242 4242 4242 4242"
          value={tarjeta.numero}
          onChange={(e) => actualizar('numero', formatearNumeroTarjeta(e.target.value))}
          disabled={deshabilitado}
          maxLength={23}
        />
        {erroresCampo.numero ? <span className="ayuda-campo-error">{erroresCampo.numero}</span> : null}
      </label>

      <label>
        Titular
        <input
          autoComplete="cc-name"
          placeholder="Como figura en la tarjeta"
          value={tarjeta.titular}
          onChange={(e) => actualizar('titular', e.target.value)}
          disabled={deshabilitado}
        />
        {erroresCampo.titular ? <span className="ayuda-campo-error">{erroresCampo.titular}</span> : null}
      </label>

      <div className="fila-datos">
        <label>
          Vencimiento
          <input
            inputMode="numeric"
            autoComplete="cc-exp"
            placeholder="MM/AA"
            value={tarjeta.vencimiento}
            onChange={(e) => actualizar('vencimiento', formatearVencimiento(e.target.value))}
            disabled={deshabilitado}
            maxLength={5}
          />
          {erroresCampo.vencimiento ? <span className="ayuda-campo-error">{erroresCampo.vencimiento}</span> : null}
        </label>

        <label>
          Codigo de seguridad
          <input
            type="password"
            inputMode="numeric"
            autoComplete="cc-csc"
            placeholder="CVV"
            value={tarjeta.codigoSeguridad}
            onChange={(e) => actualizar('codigoSeguridad', e.target.value.replace(/\D/g, '').slice(0, 4))}
            disabled={deshabilitado}
            maxLength={4}
          />
          {erroresCampo.codigoSeguridad ? (
            <span className="ayuda-campo-error">{erroresCampo.codigoSeguridad}</span>
          ) : null}
        </label>
      </div>
    </div>
  );
}
